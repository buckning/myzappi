package com.amcglynn.myzappi.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.alexa.presentation.apl.RenderDocumentDirective;
import com.amazon.ask.request.RequestHelper;
import com.amcglynn.myzappi.UserIdResolverFactory;
import com.amcglynn.myzappi.UserZoneResolver;
import com.amcglynn.myzappi.core.Brand;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.service.EnergyUsageGraphGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;
import static com.amcglynn.myzappi.LocalisedResponse.cardResponse;
import static com.amcglynn.myzappi.LocalisedResponse.voiceResponse;

@Slf4j
public class GetEnergyUsageGraphHandler implements RequestHandler {

    private final MyEnergiService.Builder zappyServiceBuilder;
    private final UserIdResolverFactory userIdResolverFactory;
    private final UserZoneResolver userZoneResolver;

    public GetEnergyUsageGraphHandler(MyEnergiService.Builder zappyServiceBuilder, UserIdResolverFactory userIdResolverFactory,
                                      UserZoneResolver userZoneResolver) {
        this.zappyServiceBuilder = zappyServiceBuilder;
        this.userIdResolverFactory = userIdResolverFactory;
        this.userZoneResolver = userZoneResolver;
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(intentName("GetEnergyUsageGraph"));
    }

    @SneakyThrows
    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        if (!hasDisplayInterface(handlerInput)) {
            return handlerInput.getResponseBuilder()
                    .withSpeech(voiceResponse(handlerInput, "graph-not-supported"))
                    .withSimpleCard(Brand.NAME,
                            cardResponse(handlerInput, "graph-not-supported"))
                    .withShouldEndSession(true)
                    .build();
        }

        var locale = Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale());
        // expected date format is 2023-05-06
        var date = parseDate(handlerInput);
        if (date.isPresent() && date.get().length() != 10) {
            return getInvalidInputResponse(handlerInput);
        }

        var userTimeZone = userZoneResolver.getZoneId(handlerInput);
        var localDate = date.map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_DATE))
                .orElse(LocalDate.now(userTimeZone));

        if (isInvalid(localDate, userTimeZone)) {
            return getInvalidRequestedDateResponse(handlerInput);
        }

        var zappiService = zappyServiceBuilder.build(userIdResolverFactory.newUserIdResolver(handlerInput)).getZappiServiceOrThrow();
        var history = zappiService.getRawEnergyHistory(localDate, userTimeZone);

        var imageContent = new EnergyUsageGraphGenerator().generateGraph(history);
        var imageSourceUrl = uploadToS3(imageContent);



        RenderDocumentDirective renderDocumentDirective = RenderDocumentDirective.builder()
                .withToken("zappidaysummaryToken")
                .withDocument(buildDocument(imageSourceUrl))
                .build();

        return handlerInput.getResponseBuilder()
                .withSpeech("Checkout the new autogenerated graph from s3")
                .withSimpleCard("My Zappi", "Checkout the graph")
                .addDirective(renderDocumentDirective)
                .withShouldEndSession(false)
                .build();
    }

    private String uploadToS3(byte[] imageBytes) {
        // Define bucket name, key (file name), and region
        String bucketName = "myzappi-autogenerated-graphs"; // Replace with your bucket name
        String keyName = "awesomefiledude.png"; // The name of the file in S3
        Region region = Region.EU_WEST_1;       // Replace with your region

        String url = null;

        // Create an S3 client
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();

        // Upload the image
        try {
            url = uploadToS3(s3, bucketName, keyName, imageBytes);
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        } finally {
            // Close the S3 client when done
            s3.close();
            return url;
        }
    }

    public String uploadToS3(S3Client s3, String bucketName, String keyName, byte[] imageData) {
        // Create an InputStream from the byte array
        InputStream imageInputStream = new ByteArrayInputStream(imageData);

        // Create a PutObjectRequest
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .contentType("image/png") // Or image/png, depending on the image format
                .build();

        // Upload the file to S3
        var response = s3.putObject(objectRequest, RequestBody.fromInputStream(imageInputStream, imageData.length));

        S3Presigner presigner = S3Presigner.builder()
                .region(Region.EU_WEST_1)
                .build();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();

        // Create a Presign request with a 10-minute expiration
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofMinutes(10)) // Set the presigned URL validity to 10 minutes
                .build();

        // Generate the presigned URL
        log.info("Presigned URL = {}", presigner.presignGetObject(presignRequest).url());

        System.out.println("File uploaded to S3 successfully.");
        return presigner.presignGetObject(presignRequest).url().toString();
    }

    @SneakyThrows
    private Map<String, Object> buildDocument(String imageSourceUrl) {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, Object>> documentMapType = new TypeReference<>() {
        };

        System.out.println("Reading the document from the resources folder");
        // Getting the resource as an InputStream
        InputStream inputStream = GetEnergyUsageGraphHandler.class.getClassLoader().getResourceAsStream("apl-energy-usage-graph.json");


        System.out.println("Loading file contents");
        // Convert InputStream to String using java.nio.charset.StandardCharsets.UTF_8
        var contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("contents = " + contents);
        System.out.println("Swapping out ${payload.backgroundEnergyUsageGraph} with " + imageSourceUrl);
        contents = contents.replace("${payload.backgroundEnergyUsageGraph}", imageSourceUrl);

        System.out.println("contents = " + contents);
        return mapper.readValue(contents,
                documentMapType);
    }

    private boolean hasDisplayInterface(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSupportedInterfaces()
                .getAlexaPresentationAPL() != null;
    }

    private Optional<Response> getInvalidRequestedDateResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "invalid-future-date"))
                .withSimpleCard(Brand.NAME,
                        cardResponse(handlerInput, "invalid-future-date"))
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> getInvalidInputResponse(HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech(voiceResponse(handlerInput, "request-specific-date"))
                .withSimpleCard(Brand.NAME,
                        cardResponse(handlerInput, "request-specific-date"))
                .withShouldEndSession(false)
                .build();
    }

    /**
     * Invalid if date is in the future. Requesting for today is accepted but not future dates.
     * @param date today or historical date
     * @param userTimeZone time-zone of the user
     * @return true if invalid
     */
    private boolean isInvalid(LocalDate date, ZoneId userTimeZone) {
        return date.isAfter(LocalDate.now(userTimeZone));
    }

    private Optional<String> parseDate(HandlerInput handlerInput) {
        return RequestHelper.forHandlerInput(handlerInput)
                .getSlotValue("date");
    }
}

# ASK CLI Primer

## Get the interaction model for a skill
```
ask smapi get-interaction-model -s $SKILL_ID -l $LOCALE
```

## Get the skill manifest
```
ask smapi get-skill-manifest -s $SKILL_ID -l $LOCALE
```

## Update the manifest for a skill from a local manifest file
```
ask smapi update-skill-manifest -s $SKILL_ID --manifest "file:manifest.json" --debug
```

## Get the status of a skill
```
ask smapi get-skill-status -s $SKILL_ID
```

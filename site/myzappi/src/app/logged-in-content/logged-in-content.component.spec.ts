import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { LoggedInContentComponent } from './logged-in-content.component';
import { Device } from '../device.interface';

describe('LoggedInContentComponent', () => {
  let component: LoggedInContentComponent;
  let fixture: ComponentFixture<LoggedInContentComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LoggedInContentComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(LoggedInContentComponent);
    component = fixture.componentInstance;
    spyOn(component, 'getDeploymentDetails');
  });

  it('should create', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('lists the registered devices in the Devices panel', () => {
    renderRegisteredContent([
      { serialNumber: '10000001', deviceClass: 'ZAPPI', tank1Name: '', tank2Name: '' },
      { serialNumber: '20000001', deviceClass: 'LIBBI', tank1Name: '', tank2Name: '' },
      { serialNumber: '30000001', deviceClass: 'EDDI', tank1Name: 'Hot water', tank2Name: '' }
    ]);

    const devicesPanel = fixture.nativeElement.querySelector('[data-testid="registered-devices-panel"]');

    expect(devicesPanel.textContent).toContain('Zappi');
    expect(devicesPanel.textContent).toContain('10000001');
    expect(devicesPanel.textContent).toContain('Libbi');
    expect(devicesPanel.textContent).toContain('20000001');
    expect(devicesPanel.textContent).toContain('Eddi');
    expect(devicesPanel.textContent).toContain('30000001');
    expect(devicesPanel.textContent).toContain('Hot water');
  });

  it('places the Devices panel after the Tariffs panel', () => {
    renderRegisteredContent([
      { serialNumber: '10000001', deviceClass: 'ZAPPI', tank1Name: '', tank2Name: '' }
    ]);

    const tariffPanel = fixture.nativeElement.querySelector('app-tariff-panel');
    const devicesPanel = fixture.nativeElement.querySelector('[data-testid="registered-devices-panel"]');

    expect(tariffPanel.compareDocumentPosition(devicesPanel) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('uses dark text in registered device rows', () => {
    renderRegisteredContent([
      { serialNumber: '10000001', deviceClass: 'ZAPPI', tank1Name: '', tank2Name: '' }
    ]);

    const deviceType = fixture.nativeElement.querySelector('.registeredDeviceType');

    expect(getComputedStyle(deviceType).color).toBe('rgb(51, 51, 51)');
  });

  function renderRegisteredContent(devices: Device[]): void {
    component.registered = true;
    component.hubDetails = devices;
    component.devices = devices;
    component.loadingDevices = false;
    fixture.detectChanges();
  }
});

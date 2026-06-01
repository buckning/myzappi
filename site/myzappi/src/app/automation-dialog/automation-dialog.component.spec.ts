import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AutomationDialogComponent } from './automation-dialog.component';

describe('AutomationDialogComponent', () => {
  let component: AutomationDialogComponent;
  let fixture: ComponentFixture<AutomationDialogComponent>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AutomationDialogComponent>>;

  beforeEach(() => {
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    TestBed.configureTestingModule({
      imports: [FormsModule],
      declarations: [AutomationDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            bearerToken: 'Bearer token',
            hubDetails: [{ serialNumber: '10000001', deviceClass: 'ZAPPI', tank1Name: '', tank2Name: '' }],
            options: {
              predicates: [{ type: 'ENERGY_EXPORTING_KW', valueType: 'DECIMAL', requiresTarget: false }],
              operators: ['GREATER_THAN'],
              actions: [{ type: 'setChargeMode', valueType: 'ENUM', deviceClass: 'ZAPPI', allowedValues: ['ECO_PLUS'] }]
            }
          }
        }
      ]
    });
    fixture = TestBed.createComponent(AutomationDialogComponent);
    component = fixture.componentInstance;
  });

  it('creates a modal payload from selected options', () => {
    fixture.detectChanges();
    component.onPredicateValueChange(2);

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({
      predicate: jasmine.objectContaining({ type: 'ENERGY_EXPORTING_KW' }),
      action: jasmine.objectContaining({ type: 'setChargeMode' })
    }));
    const payload = dialogRef.close.calls.mostRecent().args[0];
    expect(Object.prototype.hasOwnProperty.call(payload, 'name')).toBeFalse();
  });

  it('does not render a name field', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#automation-name')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Automation Details');
  });

  it('renders user-facing labels while keeping API values in the payload', () => {
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;

    expect(text).toContain('Exporting');
    expect(text).toContain('Greater than');
    expect(text).toContain('Set charge mode');
    expect(text).toContain('Eco+');
    expect(text).not.toContain('ENERGY_EXPORTING_KW');
    expect(text).not.toContain('GREATER_THAN');
    expect(text).not.toContain('setChargeMode');
    expect(text).not.toContain('ECO_PLUS');
  });

  it('hides device-specific options when the user does not own that device class', () => {
    component.data.options = {
      predicates: [
        { type: 'ENERGY_EXPORTING_KW', valueType: 'DECIMAL', requiresTarget: false },
        { type: 'ZAPPI_EV_CHARGE_RATE_KW', valueType: 'DECIMAL', requiresTarget: true, deviceClass: 'ZAPPI' },
        { type: 'LIBBI_STATE_OF_CHARGE_PERCENT', valueType: 'DECIMAL', requiresTarget: true, deviceClass: 'LIBBI' }
      ],
      operators: ['GREATER_THAN'],
      actions: [
        { type: 'setChargeMode', valueType: 'ENUM', deviceClass: 'ZAPPI', allowedValues: ['ECO_PLUS'] },
        { type: 'setEddiMode', valueType: 'ENUM', deviceClass: 'EDDI', allowedValues: ['NORMAL'] },
        { type: 'setLibbiEnabled', valueType: 'BOOLEAN', deviceClass: 'LIBBI', allowedValues: ['true', 'false'] }
      ]
    };
    component.data.hubDetails = [{ serialNumber: '10000001', deviceClass: 'ZAPPI', tank1Name: '', tank2Name: '' }];

    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Exporting');
    expect(text).toContain('EV charge rate');
    expect(text).toContain('Set charge mode');
    expect(text).not.toContain('Libbi state of charge');
    expect(text).not.toContain('Set Eddi mode');
    expect(text).not.toContain('Enable Libbi');
  });

  it('uses implicit device targets when there is only one matching device', () => {
    component.data.options = {
      predicates: [
        { type: 'ZAPPI_EV_CHARGE_RATE_KW', valueType: 'DECIMAL', requiresTarget: true, deviceClass: 'ZAPPI' }
      ],
      operators: ['GREATER_THAN'],
      actions: [
        { type: 'setChargeMode', valueType: 'ENUM', deviceClass: 'ZAPPI', allowedValues: ['ECO_PLUS'] }
      ]
    };
    component.data.hubDetails = [{ serialNumber: '10000001', deviceClass: 'ZAPPI', tank1Name: '', tank2Name: '' }];
    fixture.detectChanges();
    component.onPredicateValueChange(2);

    expect(fixture.nativeElement.querySelector('#predicate-target')).toBeNull();
    expect(fixture.nativeElement.querySelector('#action-target')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Zappi 10000001');

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({
      predicate: jasmine.objectContaining({ target: '10000001' }),
      action: jasmine.objectContaining({ target: '10000001' })
    }));
  });

  it('keeps device target dropdowns when there are multiple matching devices', () => {
    component.data.options = {
      predicates: [
        { type: 'ZAPPI_EV_CHARGE_RATE_KW', valueType: 'DECIMAL', requiresTarget: true, deviceClass: 'ZAPPI' }
      ],
      operators: ['GREATER_THAN'],
      actions: [
        { type: 'setChargeMode', valueType: 'ENUM', deviceClass: 'ZAPPI', allowedValues: ['ECO_PLUS'] }
      ]
    };
    component.data.hubDetails = [
      { serialNumber: '10000001', deviceClass: 'ZAPPI', tank1Name: '', tank2Name: '' },
      { serialNumber: '10000002', deviceClass: 'ZAPPI', tank1Name: '', tank2Name: '' }
    ];

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#predicate-target')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#action-target')).not.toBeNull();
  });

  it('uses a number input for numeric predicate values while submitting string values', () => {
    component.data.options = {
      predicates: [{ type: 'ENERGY_EXPORTING_KW', valueType: 'DECIMAL', requiresTarget: false }],
      operators: ['GREATER_THAN'],
      actions: [{ type: 'setChargeMode', valueType: 'ENUM', deviceClass: 'ZAPPI', allowedValues: ['ECO_PLUS'] }]
    };
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('#predicate-value') as HTMLInputElement;
    expect(input.type).toEqual('number');
    expect(input.step).toEqual('any');

    component.onPredicateValueChange(2.5);
    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({
      predicate: jasmine.objectContaining({ value: '2.5' })
    }));
  });

  it('shows kW on the predicate value label for power metrics', () => {
    component.data.options = {
      predicates: [{ type: 'ENERGY_IMPORTING_KW', valueType: 'DECIMAL', requiresTarget: false }],
      operators: ['GREATER_THAN'],
      actions: [{ type: 'setChargeMode', valueType: 'ENUM', deviceClass: 'ZAPPI', allowedValues: ['ECO_PLUS'] }]
    };
    fixture.detectChanges();

    const label = fixture.nativeElement.querySelector('label[for="predicate-value"]') as HTMLLabelElement;
    expect(label.textContent?.trim()).toEqual('Value (kW)');
  });

  it('shows percent on the predicate value label for percentage metrics', () => {
    component.data.options = {
      predicates: [
        { type: 'LIBBI_STATE_OF_CHARGE_PERCENT', valueType: 'DECIMAL', requiresTarget: true, deviceClass: 'LIBBI' }
      ],
      operators: ['GREATER_THAN'],
      actions: [{ type: 'setChargeMode', valueType: 'ENUM', deviceClass: 'ZAPPI', allowedValues: ['ECO_PLUS'] }]
    };
    component.data.hubDetails = [
      { serialNumber: '10000001', deviceClass: 'ZAPPI', tank1Name: '', tank2Name: '' },
      { serialNumber: '20000001', deviceClass: 'LIBBI', tank1Name: '', tank2Name: '' }
    ];
    fixture.detectChanges();

    const label = fixture.nativeElement.querySelector('label[for="predicate-value"]') as HTMLLabelElement;
    expect(label.textContent?.trim()).toEqual('Value (%)');
  });

  it('shows percent on the action value label for minimum green level', () => {
    component.data.options = {
      predicates: [{ type: 'ENERGY_EXPORTING_KW', valueType: 'DECIMAL', requiresTarget: false }],
      operators: ['GREATER_THAN'],
      actions: [{ type: 'setZappiMgl', valueType: 'INTEGER', deviceClass: 'ZAPPI', min: 1, max: 100 }]
    };
    fixture.detectChanges();

    const label = fixture.nativeElement.querySelector('label[for="action-value-input"]') as HTMLLabelElement;
    expect(label.textContent?.trim()).toEqual('Action value (%)');
  });

  it('does not show a unit on the action value label for charge mode', () => {
    fixture.detectChanges();

    const label = fixture.nativeElement.querySelector('label[for="action-value-select"]') as HTMLLabelElement;
    expect(label.textContent?.trim()).toEqual('Action value');
  });

  it('uses a text input for non-numeric predicate values', () => {
    component.data.options = {
      predicates: [{ type: 'TEST_STATUS', valueType: 'STRING', requiresTarget: false }],
      operators: ['GREATER_THAN'],
      actions: [{ type: 'setChargeMode', valueType: 'ENUM', deviceClass: 'ZAPPI', allowedValues: ['ECO_PLUS'] }]
    };
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('#predicate-value') as HTMLInputElement;
    expect(input.type).toEqual('text');
    expect(input.getAttribute('step')).toBeNull();
  });

  it('disables save and shows a validation message when the predicate value is missing', () => {
    fixture.detectChanges();

    const saveButton = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(saveButton.disabled).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Value is required.');

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('enables save when required automation fields are valid', () => {
    fixture.detectChanges();

    component.onPredicateValueChange(2.5);
    fixture.detectChanges();

    const saveButton = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(saveButton.disabled).toBeFalse();
    expect(fixture.nativeElement.textContent).not.toContain('Value is required.');
  });

  it('disables save when a numeric predicate value is invalid', () => {
    fixture.detectChanges();

    component.onPredicateValueChange('not-a-number');
    fixture.detectChanges();

    const saveButton = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(saveButton.disabled).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Value must be a number.');
  });

  it('uses the footer cancel button as the only cancel control', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.close-button')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Cancel');
  });
});

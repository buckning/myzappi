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
    component.name = 'Solar export';
    component.predicate.type = 'ENERGY_EXPORTING_KW';
    component.predicate.operator = 'GREATER_THAN';
    component.predicate.value = '2.0';
    component.action.type = 'setChargeMode';
    component.action.target = '10000001';
    component.action.value = 'ECO_PLUS';

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({
      name: 'Solar export',
      predicate: jasmine.objectContaining({ type: 'ENERGY_EXPORTING_KW' }),
      action: jasmine.objectContaining({ type: 'setChargeMode' })
    }));
  });
});

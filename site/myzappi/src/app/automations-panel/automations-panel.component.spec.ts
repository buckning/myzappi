import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { AutomationsPanelComponent } from './automations-panel.component';
import { AutomationService } from '../automation.service';

describe('AutomationsPanelComponent', () => {
  let component: AutomationsPanelComponent;
  let fixture: ComponentFixture<AutomationsPanelComponent>;
  let automationService: jasmine.SpyObj<AutomationService>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const options = {
    predicates: [{ type: 'ENERGY_EXPORTING_KW', valueType: 'DECIMAL', requiresTarget: false }],
    operators: ['GREATER_THAN', 'LESS_THAN'],
    actions: [{ type: 'setChargeMode', valueType: 'ENUM', deviceClass: 'ZAPPI', allowedValues: ['ECO_PLUS'] }]
  };
  const automation = {
    automationId: 'a',
    active: true,
    priority: 1,
    predicate: { type: 'ENERGY_EXPORTING_KW', operator: 'GREATER_THAN' as const, value: '2.0' },
    action: { type: 'setChargeMode', target: '10000001', value: 'ECO_PLUS' }
  };

  beforeEach(() => {
    automationService = jasmine.createSpyObj('AutomationService', ['getOptions', 'list', 'create', 'setActive', 'delete', 'reorder']);
    dialog = jasmine.createSpyObj('MatDialog', ['open']);
    automationService.getOptions.and.returnValue(of(options));
    automationService.list.and.returnValue(of({ automations: [automation] }));
    automationService.create.and.returnValue(of(automation));
    automationService.setActive.and.returnValue(of(undefined));
    automationService.delete.and.returnValue(of(undefined));
    automationService.reorder.and.returnValue(of(undefined));

    TestBed.configureTestingModule({
      declarations: [AutomationsPanelComponent],
      providers: [
        { provide: AutomationService, useValue: automationService },
        { provide: MatDialog, useValue: dialog }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    });
    fixture = TestBed.createComponent(AutomationsPanelComponent);
    component = fixture.componentInstance;
    component.bearerToken = 'Bearer token';
    component.hubDetails = [{ serialNumber: '10000001', deviceClass: 'ZAPPI', tank1Name: '', tank2Name: '' }];
  });

  it('loads automations and options on init', () => {
    fixture.detectChanges();

    expect(component.loaded).toBeTrue();
    expect(component.automations.length).toBe(1);
    expect(automationService.getOptions).toHaveBeenCalledWith('Bearer token');
    expect(automationService.list).toHaveBeenCalledWith('Bearer token');
  });

  it('opens the create automation dialog', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(null) } as any);
    fixture.detectChanges();

    component.openCreateDialog();

    expect(dialog.open).toHaveBeenCalled();
  });

  it('creates an automation through the service', () => {
    dialog.open.and.returnValue({ afterClosed: () => of({ name: 'Solar export' }) } as any);
    fixture.detectChanges();

    component.openCreateDialog();

    expect(automationService.create).toHaveBeenCalledWith('Bearer token', { name: 'Solar export' });
  });

  it('toggles an automation active state', () => {
    fixture.detectChanges();

    component.toggleActive(component.automations[0]);

    expect(automationService.setActive).toHaveBeenCalledWith('Bearer token', 'a', false);
  });

  it('deletes an automation', () => {
    fixture.detectChanges();

    component.deleteAutomation(component.automations[0]);

    expect(automationService.delete).toHaveBeenCalledWith('Bearer token', 'a');
  });

  it('sends normalized ordered ids after drag and drop', () => {
    automationService.list.and.returnValue(of({ automations: [automation, { ...automation, automationId: 'b', priority: 2 }] }));
    fixture.detectChanges();

    component.drop({ previousIndex: 1, currentIndex: 0 } as any);

    expect(automationService.reorder).toHaveBeenCalledWith('Bearer token', ['b', 'a']);
  });

  it('renders name when present and summary when name is blank', () => {
    automationService.list.and.returnValue(of({ automations: [{ ...automation, name: 'Named automation' }, automation] }));
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Named automation');
    expect(text).toContain('ENERGY_EXPORTING_KW GREATER_THAN 2.0 then setChargeMode ECO_PLUS');
  });

  it('does not render runtime state fields', () => {
    automationService.list.and.returnValue(of({ automations: [{ ...automation, lastTriggeredAt: '2026-05-30T12:00' } as any] }));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('lastTriggeredAt');
    expect(fixture.nativeElement.textContent).not.toContain('2026-05-30T12:00');
  });
});

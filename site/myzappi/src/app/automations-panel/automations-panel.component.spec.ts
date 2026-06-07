import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, Subject, throwError } from 'rxjs';
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

  it('renders the beta title and plain-language feature description', () => {
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Automations (Beta)');
    expect(text).toContain('Automations let MyZappi check your myenergi setup every few minutes.');
    expect(text).toContain('MyZappi can automatically run the action you selected');
  });

  it('opens the create automation dialog', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(null) } as any);
    fixture.detectChanges();

    component.openCreateDialog();

    expect(dialog.open).toHaveBeenCalledWith(jasmine.any(Function), jasmine.objectContaining({
      panelClass: 'automation-dialog-panel',
      disableClose: true
    }));
  });

  it('creates an automation through the service', () => {
    const payload = { predicate: automation.predicate, action: automation.action };
    dialog.open.and.returnValue({ afterClosed: () => of(payload) } as any);
    fixture.detectChanges();

    component.openCreateDialog();

    expect(automationService.create).toHaveBeenCalledWith('Bearer token', payload);
  });

  it('shows an error when creating an automation fails', () => {
    const payload = { predicate: automation.predicate, action: automation.action };
    dialog.open.and.returnValue({ afterClosed: () => of(payload) } as any);
    automationService.create.and.returnValue(throwError(() => ({ status: 400 })));
    fixture.detectChanges();

    component.openCreateDialog();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Could not save automation. Check the automation details and try again.');
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

  it('removes the final automation from the visible list after delete succeeds', () => {
    const deleteCompleted = new Subject<void>();
    automationService.delete.and.returnValue(deleteCompleted.asObservable());
    fixture.detectChanges();

    component.deleteAutomation(component.automations[0]);

    expect(component.automations.length).toBe(1);

    deleteCompleted.next();
    deleteCompleted.complete();
    fixture.detectChanges();

    expect(component.automations.length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('You have no automations. Create a new automation below.');
  });

  it('sends normalized ordered ids after drag and drop', () => {
    automationService.list.and.returnValue(of({ automations: [automation, { ...automation, automationId: 'b', priority: 2 }] }));
    fixture.detectChanges();

    component.drop({ previousIndex: 1, currentIndex: 0 } as any);

    expect(automationService.reorder).toHaveBeenCalledWith('Bearer token', ['b', 'a']);
  });

  it('renders rule summaries without visible priority or name text', () => {
    automationService.list.and.returnValue(of({ automations: [{ ...automation, name: 'Named automation' }, automation] }));
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).not.toContain('Named automation');
    expect(text).not.toContain('Priority 1');
    expect(text).not.toContain('Priority 2');
    expect(text).toContain('Exporting Greater than 2.0 kW then Set charge mode Eco+');
  });

  it('renders percentage units in saved automation summaries', () => {
    automationService.list.and.returnValue(of({
      automations: [{
        ...automation,
        predicate: {
          type: 'LIBBI_STATE_OF_CHARGE_PERCENT',
          target: '20000001',
          operator: 'LESS_THAN' as const,
          value: '80'
        },
        action: { type: 'setZappiMgl', target: '10000001', value: '45' }
      }]
    }));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent)
      .toContain('Libbi state of charge 20000001 Less than 80% then Set minimum green level 45%');
  });

  it('does not add units to unitless saved automation actions', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Set charge mode Eco+');
    expect(fixture.nativeElement.textContent).not.toContain('Eco+ %');
  });

  it('does not render runtime state fields', () => {
    automationService.list.and.returnValue(of({ automations: [{ ...automation, lastTriggeredAt: '2026-05-30T12:00' } as any] }));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('lastTriggeredAt');
    expect(fixture.nativeElement.textContent).not.toContain('2026-05-30T12:00');
  });
});

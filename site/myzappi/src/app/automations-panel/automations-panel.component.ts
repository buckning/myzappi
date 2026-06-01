import { Component, Input, OnInit } from '@angular/core';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { MatDialog } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';
import { Automation, AutomationAction, AutomationOptions, AutomationPredicate } from '../automation.interface';
import { AutomationService } from '../automation.service';
import { Device } from '../device.interface';
import { AutomationDialogComponent } from '../automation-dialog/automation-dialog.component';
import {
  actionLabel,
  automationUnitLabel,
  automationValueLabel,
  operatorLabel,
  predicateLabel
} from '../automation-labels';

@Component({
  selector: 'app-automations-panel',
  templateUrl: './automations-panel.component.html',
  styleUrls: ['./automations-panel.component.css']
})
export class AutomationsPanelComponent implements OnInit {
  @Input() public bearerToken: any;
  @Input() public hubDetails: Device[] = [];

  loaded = false;
  automations: Automation[] = [];
  options?: AutomationOptions;
  errorMessage = '';

  constructor(private automationService: AutomationService, private dialog: MatDialog) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loaded = false;
    this.errorMessage = '';
    forkJoin({
      options: this.automationService.getOptions(this.bearerToken),
      automations: this.automationService.list(this.bearerToken)
    }).subscribe(result => {
      this.options = result.options;
      this.automations = result.automations.automations.sort((a, b) => a.priority - b.priority);
      this.loaded = true;
    });
  }

  openCreateDialog(): void {
    if (!this.options) {
      return;
    }
    const dialogRef = this.dialog.open(AutomationDialogComponent, {
      width: '90%',
      maxWidth: '520px',
      panelClass: 'automation-dialog-panel',
      disableClose: true,
      autoFocus: false,
      data: {
        bearerToken: this.bearerToken,
        hubDetails: this.hubDetails,
        options: this.options
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (!result) {
        return;
      }
      this.errorMessage = '';
      this.automationService.create(this.bearerToken, result).subscribe({
        next: () => this.load(),
        error: () => {
          this.errorMessage = 'Could not save automation. Check the automation details and try again.';
        }
      });
    });
  }

  toggleActive(automation: Automation): void {
    const active = !automation.active;
    this.automationService.setActive(this.bearerToken, automation.automationId, active).subscribe(() => {
      automation.active = active;
    });
  }

  deleteAutomation(automation: Automation): void {
    this.automationService.delete(this.bearerToken, automation.automationId).subscribe(() => {
      this.automations = this.automations
        .filter(current => current.automationId !== automation.automationId)
        .map((current, index) => ({ ...current, priority: index + 1 }));
    });
  }

  drop(event: CdkDragDrop<Automation[]>): void {
    moveItemInArray(this.automations, event.previousIndex, event.currentIndex);
    this.automations = this.automations.map((automation, index) => ({ ...automation, priority: index + 1 }));
    this.automationService.reorder(this.bearerToken, this.automations.map(automation => automation.automationId)).subscribe();
  }

  summary(automation: Automation): string {
    return `${this.predicateLabel(automation.predicate)} then ${this.actionLabel(automation.action)}`;
  }

  predicateLabel(predicate: AutomationPredicate): string {
    const target = predicate.target ? ` ${predicate.target}` : '';
    return `${predicateLabel(predicate.type)}${target} ${operatorLabel(predicate.operator)} ${this.valueWithUnit(
      predicate.value,
      automationUnitLabel(predicate.type)
    )}`;
  }

  actionLabel(action: AutomationAction): string {
    return `${actionLabel(action.type)} ${this.valueWithUnit(
      automationValueLabel(action.value),
      automationUnitLabel(action.type)
    )}`;
  }

  private valueWithUnit(value: string, unit: string): string {
    if (!unit) {
      return value;
    }
    return unit === '%' ? `${value}%` : `${value} ${unit}`;
  }
}

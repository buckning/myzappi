import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AutomationAction, AutomationOptions, AutomationPredicate } from '../automation.interface';
import { Device } from '../device.interface';

export interface AutomationDialogData {
  bearerToken: string;
  hubDetails: Device[];
  options: AutomationOptions;
}

@Component({
  selector: 'app-automation-dialog',
  templateUrl: './automation-dialog.component.html',
  styleUrls: ['./automation-dialog.component.css']
})
export class AutomationDialogComponent implements OnInit {
  name = '';
  predicate: AutomationPredicate = {
    type: '',
    operator: 'GREATER_THAN',
    value: ''
  };
  action: AutomationAction = {
    type: '',
    target: '',
    value: ''
  };

  constructor(
    private dialogRef: MatDialogRef<AutomationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AutomationDialogData
  ) {}

  ngOnInit(): void {
    this.predicate.type = this.data.options.predicates[0]?.type || '';
    this.predicate.operator = (this.data.options.operators[0] as 'GREATER_THAN' | 'LESS_THAN') || 'GREATER_THAN';
    this.onPredicateTypeChange();
    this.action.type = this.data.options.actions[0]?.type || '';
    this.applySelectedActionDefaults();
  }

  selectedPredicate() {
    return this.data.options.predicates.find(predicate => predicate.type === this.predicate.type);
  }

  selectedAction() {
    return this.data.options.actions.find(action => action.type === this.action.type);
  }

  predicateTargets(): Device[] {
    const selected = this.selectedPredicate();
    if (!selected?.requiresTarget || !selected.deviceClass) {
      return [];
    }
    return this.devicesForClass(selected.deviceClass);
  }

  actionTargets(): Device[] {
    const selected = this.selectedAction();
    if (!selected) {
      return [];
    }
    return this.devicesForClass(selected.deviceClass);
  }

  devicesForClass(deviceClass: string): Device[] {
    return this.data.hubDetails.filter(device => device.deviceClass === deviceClass);
  }

  onPredicateTypeChange(): void {
    const selected = this.selectedPredicate();
    if (!selected?.requiresTarget) {
      delete this.predicate.target;
      return;
    }
    this.predicate.target = this.predicateTargets()[0]?.serialNumber;
  }

  onActionTypeChange(): void {
    this.applySelectedActionDefaults();
  }

  submit(): void {
    const selectedPredicate = this.selectedPredicate();
    const predicate = { ...this.predicate };
    if (!selectedPredicate?.requiresTarget) {
      delete predicate.target;
    }
    this.dialogRef.close({
      name: this.name.trim() || undefined,
      predicate,
      action: { ...this.action }
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  private applySelectedActionDefaults(): void {
    const selected = this.selectedAction();
    this.action.target = selected ? this.actionTargets()[0]?.serialNumber || '' : '';
    this.action.value = selected?.allowedValues?.[0] || selected?.min?.toString() || '';
  }
}

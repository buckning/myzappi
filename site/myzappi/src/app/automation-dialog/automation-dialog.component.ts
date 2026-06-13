import { Component, Inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ActionOption, AutomationAction, AutomationOptions, AutomationPredicate } from '../automation.interface';
import { Device } from '../device.interface';
import {
  actionLabel,
  automationUnitLabel,
  automationValueLabel,
  deviceClassLabel,
  operatorLabel,
  predicateLabel
} from '../automation-labels';

export interface AutomationDialogData {
  bearerToken: string;
  hubDetails: Device[];
  options: AutomationOptions;
}

@Component({
    selector: 'app-automation-dialog',
    templateUrl: './automation-dialog.component.html',
    styleUrls: ['./automation-dialog.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class AutomationDialogComponent implements OnInit {
  options: AutomationOptions = {
    predicates: [],
    operators: [],
    actions: []
  };
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
    this.options = this.optionsForOwnedDevices(this.data.options);
    this.predicate.type = this.options.predicates[0]?.type || '';
    this.predicate.operator = (this.options.operators[0] as 'GREATER_THAN' | 'LESS_THAN') || 'GREATER_THAN';
    this.onPredicateTypeChange();
    this.action.type = this.options.actions[0]?.type || '';
    this.applySelectedActionDefaults();
  }

  selectedPredicate() {
    return this.options.predicates.find(predicate => predicate.type === this.predicate.type);
  }

  selectedAction() {
    return this.options.actions.find(action => action.type === this.action.type);
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
    return this.data.hubDetails.filter(device => this.matchesDeviceClass(device.deviceClass, deviceClass));
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

  onActionValueChange(value: string | number | null): void {
    this.action.value = value === null ? '' : String(value);
  }

  onPredicateValueChange(value: string | number | null): void {
    this.predicate.value = value === null ? '' : String(value);
  }

  predicateValueInputType(): string {
    return this.isNumericPredicateValue() ? 'number' : 'text';
  }

  predicateValueStep(): string | null {
    if (!this.isNumericPredicateValue()) {
      return null;
    }
    return this.selectedPredicate()?.valueType === 'INTEGER' ? '1' : 'any';
  }

  isNumericPredicateValue(): boolean {
    return this.isNumericValueType(this.selectedPredicate()?.valueType);
  }

  predicateDisplayLabel(type: string): string {
    return predicateLabel(type);
  }

  operatorDisplayLabel(operator: string): string {
    return operatorLabel(operator);
  }

  actionDisplayLabel(type: string): string {
    return actionLabel(type);
  }

  valueDisplayLabel(value: string): string {
    return automationValueLabel(value);
  }

  predicateValueLabel(): string {
    return this.labelWithUnit('Value', automationUnitLabel(this.predicate.type));
  }

  actionValueLabel(): string {
    return this.labelWithUnit('Action value', automationUnitLabel(this.action.type));
  }

  deviceLabel(device: Device): string {
    return `${deviceClassLabel(device.deviceClass)} ${device.serialNumber}`;
  }

  hasMultiplePredicateTargets(): boolean {
    return this.predicateTargets().length > 1;
  }

  hasMultipleActionTargets(): boolean {
    return this.actionTargets().length > 1;
  }

  selectedPredicateTargetLabel(): string {
    return this.selectedDeviceLabel(this.predicate.target, this.predicateTargets());
  }

  selectedActionTargetLabel(): string {
    return this.selectedDeviceLabel(this.action.target, this.actionTargets());
  }

  predicateValueError(): string | null {
    if (!this.hasValue(this.predicate.value)) {
      return 'Value is required.';
    }
    if (this.isNumericPredicateValue() && !this.isFiniteNumber(this.predicate.value)) {
      return 'Value must be a number.';
    }
    return null;
  }

  actionValueError(): string | null {
    const selected = this.selectedAction();
    if (!selected || !this.hasValue(this.action.value)) {
      return 'Action value is required.';
    }
    if (selected.allowedValues?.length && !selected.allowedValues.includes(String(this.action.value))) {
      return 'Select a supported action value.';
    }
    if (this.isNumericValueType(selected.valueType)) {
      if (!this.isFiniteNumber(this.action.value)) {
        return 'Action value must be a number.';
      }
      const value = Number(this.action.value);
      if (selected.valueType === 'INTEGER' && !Number.isInteger(value)) {
        return 'Action value must be a whole number.';
      }
      if (selected.min !== undefined && value < selected.min) {
        return `Action value must be at least ${selected.min}.`;
      }
      if (selected.max !== undefined && value > selected.max) {
        return `Action value must be no more than ${selected.max}.`;
      }
    }
    return null;
  }

  canSave(): boolean {
    const selectedPredicate = this.selectedPredicate();
    const selectedAction = this.selectedAction();
    if (!selectedPredicate || !selectedAction || !this.predicate.operator) {
      return false;
    }
    if (selectedPredicate.requiresTarget && !this.hasValue(this.predicate.target)) {
      return false;
    }
    if (!this.hasValue(this.action.target)) {
      return false;
    }
    return !this.predicateValueError() && !this.actionValueError();
  }

  submit(): void {
    if (!this.canSave()) {
      return;
    }
    const selectedPredicate = this.selectedPredicate();
    const predicate = { ...this.predicate };
    if (!selectedPredicate?.requiresTarget) {
      delete predicate.target;
    }
    this.dialogRef.close({
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

  private optionsForOwnedDevices(options: AutomationOptions): AutomationOptions {
    return {
      predicates: options.predicates.filter(option => this.optionMatchesOwnedDevice(option.deviceClass)),
      operators: options.operators,
      actions: options.actions.filter(option => this.optionMatchesOwnedDevice(option.deviceClass))
    };
  }

  private optionMatchesOwnedDevice(deviceClass: ActionOption['deviceClass'] | undefined): boolean {
    if (!deviceClass) {
      return true;
    }
    return this.data.hubDetails.some(device => this.matchesDeviceClass(device.deviceClass, deviceClass));
  }

  private matchesDeviceClass(actual: string, expected: string): boolean {
    return actual.toLowerCase() === expected.toLowerCase();
  }

  private isNumericValueType(valueType: string | undefined): boolean {
    return ['DECIMAL', 'DOUBLE', 'FLOAT', 'INTEGER', 'NUMBER'].includes(valueType || '');
  }

  private hasValue(value: string | number | undefined | null): boolean {
    return value !== undefined && value !== null && String(value).trim() !== '';
  }

  private isFiniteNumber(value: string | number): boolean {
    return Number.isFinite(Number(value));
  }

  private labelWithUnit(label: string, unit: string): string {
    return unit ? `${label} (${unit})` : label;
  }

  private selectedDeviceLabel(serialNumber: string | undefined, devices: Device[]): string {
    const device = devices.find(candidate => candidate.serialNumber === serialNumber) || devices[0];
    return device ? this.deviceLabel(device) : '';
  }
}

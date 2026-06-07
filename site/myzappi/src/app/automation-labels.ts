const PREDICATE_LABELS: Record<string, string> = {
  ENERGY_SOLAR_GENERATION_KW: 'Solar generation',
  ENERGY_EXPORTING_KW: 'Exporting',
  ENERGY_IMPORTING_KW: 'Importing',
  ENERGY_CONSUMING_KW: 'Consuming',
  ZAPPI_EV_CHARGE_RATE_KW: 'EV charge rate',
  LIBBI_STATE_OF_CHARGE_PERCENT: 'Libbi state of charge'
};

const OPERATOR_LABELS: Record<string, string> = {
  GREATER_THAN: 'Greater than',
  LESS_THAN: 'Less than'
};

const ACTION_LABELS: Record<string, string> = {
  setChargeMode: 'Set charge mode',
  setZappiMgl: 'Set minimum green level',
  setEddiMode: 'Set Eddi mode',
  setLibbiEnabled: 'Enable Libbi',
  setLibbiChargeFromGrid: 'Set Libbi charge from grid',
  setLibbiChargeTarget: 'Set Libbi charge target'
};

const VALUE_LABELS: Record<string, string> = {
  ECO_PLUS: 'Eco+',
  ECO: 'Eco',
  FAST: 'Fast',
  STOP: 'Stop',
  NORMAL: 'Normal',
  STOPPED: 'Stopped',
  true: 'Yes',
  false: 'No'
};

const UNIT_LABELS: Record<string, string> = {
  setZappiMgl: '%',
  setLibbiChargeTarget: '%'
};

export function predicateLabel(value: string): string {
  return labelFor(value, PREDICATE_LABELS);
}

export function operatorLabel(value: string): string {
  return labelFor(value, OPERATOR_LABELS);
}

export function actionLabel(value: string): string {
  return labelFor(value, ACTION_LABELS);
}

export function automationValueLabel(value: string): string {
  return labelFor(value, VALUE_LABELS);
}

export function automationUnitLabel(value: string): string {
  if (UNIT_LABELS[value]) {
    return UNIT_LABELS[value];
  }
  if (value.endsWith('_KW')) {
    return 'kW';
  }
  if (value.endsWith('_PERCENT')) {
    return '%';
  }
  return '';
}

export function deviceClassLabel(value: string): string {
  return titleCase(value);
}

function labelFor(value: string, labels: Record<string, string>): string {
  return labels[value] || titleCase(value);
}

function titleCase(value: string): string {
  const label = value
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .trim()
    .toLowerCase();
  return label.charAt(0).toUpperCase() + label.slice(1);
}

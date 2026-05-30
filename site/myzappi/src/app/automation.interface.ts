export interface Automation {
  automationId: string;
  name?: string;
  active: boolean;
  priority: number;
  predicate: AutomationPredicate;
  action: AutomationAction;
  createdAt?: string;
}

export interface AutomationPredicate {
  type: string;
  target?: string;
  operator: 'GREATER_THAN' | 'LESS_THAN';
  value: string;
}

export interface AutomationAction {
  type: string;
  target: string;
  value: string;
}

export interface AutomationsResponse {
  automations: Automation[];
}

export interface AutomationOptions {
  predicates: PredicateOption[];
  operators: string[];
  actions: ActionOption[];
}

export interface PredicateOption {
  type: string;
  valueType: string;
  requiresTarget: boolean;
  deviceClass?: string;
}

export interface ActionOption {
  type: string;
  valueType: string;
  deviceClass: string;
  allowedValues?: string[];
  min?: number;
  max?: number;
}

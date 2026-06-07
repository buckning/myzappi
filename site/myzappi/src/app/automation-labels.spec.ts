import { actionLabel, automationUnitLabel, automationValueLabel, operatorLabel, predicateLabel } from './automation-labels';

describe('automation labels', () => {
  it('maps automation API values to user-facing labels', () => {
    expect(predicateLabel('ENERGY_SOLAR_GENERATION_KW')).toEqual('Solar generation');
    expect(predicateLabel('ZAPPI_EV_CHARGE_RATE_KW')).toEqual('EV charge rate');
    expect(operatorLabel('GREATER_THAN')).toEqual('Greater than');
    expect(operatorLabel('LESS_THAN')).toEqual('Less than');
    expect(actionLabel('setChargeMode')).toEqual('Set charge mode');
    expect(actionLabel('setLibbiChargeFromGrid')).toEqual('Set Libbi charge from grid');
    expect(automationValueLabel('ECO_PLUS')).toEqual('Eco+');
    expect(automationValueLabel('true')).toEqual('Yes');
  });

  it('falls back to a readable label for unknown values', () => {
    expect(predicateLabel('NEW_API_VALUE')).toEqual('New api value');
    expect(actionLabel('setNewThing')).toEqual('Set new thing');
  });

  it('maps automation value units from API values', () => {
    expect(automationUnitLabel('ENERGY_IMPORTING_KW')).toEqual('kW');
    expect(automationUnitLabel('ZAPPI_EV_CHARGE_RATE_KW')).toEqual('kW');
    expect(automationUnitLabel('LIBBI_STATE_OF_CHARGE_PERCENT')).toEqual('%');
    expect(automationUnitLabel('setZappiMgl')).toEqual('%');
    expect(automationUnitLabel('setChargeMode')).toEqual('');
  });
});

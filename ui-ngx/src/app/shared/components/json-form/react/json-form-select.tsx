import * as React from 'react';
import {
  JsonFormFieldProps,
  JsonFormFieldState,
  KeyLabelItem
} from '@shared/components/json-form/react/json-form.models';
import MenuItem from '@material-ui/core/MenuItem';
import FormControl from '@material-ui/core/FormControl';
import InputLabel from '@material-ui/core/InputLabel';
import Select from '@material-ui/core/Select';
import ThingsboardBaseComponent from '@shared/components/json-form/react/json-form-base-component';

interface ThingsboardSelectState extends JsonFormFieldState {
  currentValue: any;
}

class ThingsboardSelect extends React.Component<JsonFormFieldProps, ThingsboardSelectState> {

  constructor(props) {
    super(props);
    this.onSelected = this.onSelected.bind(this);
    const possibleValue = this.getModelKey(this.props.model, this.props.form.key);
    this.state = {
      currentValue: this.props.model !== undefined && possibleValue ? possibleValue : this.props.form.titleMap != null ?
        this.props.form.titleMap[0].value : ''
    };
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.model && nextProps.form.key) {
      this.setState({
        currentValue: this.getModelKey(nextProps.model, nextProps.form.key)
          || (nextProps.form.titleMap != null ? nextProps.form.titleMap[0].value : '')
      });
    }
  }

  getModelKey(model, key) {
    if (Array.isArray(key)) {
      return key.reduce((cur, nxt) => (cur[nxt] || {}), model);
    } else {
      return model[key];
    }
  }

  onSelected(event: React.ChangeEvent<{ name?: string; value: any }>) {

    this.setState({
      currentValue: event.target.value
    });
    this.props.onChangeValidate(event);
  }

  render() {
    const menuItems = this.props.form.titleMap.map((item, idx) => (
      <MenuItem key={idx}
                value={item.value}>{item.name}</MenuItem>
    ));

    return (
      <FormControl className={this.props.form.htmlClass}
                   disabled={this.props.form.readonly}
                   fullWidth={true}>
        <InputLabel htmlFor='select-field'>{this.props.form.title}</InputLabel>
        <Select
          value={this.state.currentValue}
          onChange={this.onSelected}>
          {menuItems}
        </Select>
      </FormControl>
    );
  }
}

export default ThingsboardBaseComponent(ThingsboardSelect);

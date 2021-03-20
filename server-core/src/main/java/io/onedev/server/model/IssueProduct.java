package io.onedev.server.model;


import javax.persistence.OneToMany;
import javax.persistence.CascadeType;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import io.onedev.server.util.Input;
import java.util.stream.Collectors;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import io.onedev.server.model.support.issue.fieldspec.FieldSpec;
import edu.emory.mathcs.backport.java.util.Collections;
import java.util.Comparator;
import com.google.common.collect.Lists;
import javax.annotation.Nullable;
import java.util.Map;
import java.io.Serializable;
import io.onedev.server.web.editable.BeanDescriptor;
import io.onedev.server.web.editable.PropertyDescriptor;
import java.util.Set;
import io.onedev.server.model.support.administration.GlobalIssueSetting;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.SettingManager;

public class IssueProduct implements Serializable {
	private Collection<IssueField> fields = new ArrayList<>();
	private transient Map<String, Input> fieldInputs;

	public Collection<IssueField> getFields() {
		return fields;
	}

	public void setFields(Collection<IssueField> fields) {
		this.fields = fields;
	}

	public Collection<String> getFieldNames() {
		return fields.stream().map(it -> it.getName()).collect(Collectors.toSet());
	}

	public void removeFields(Collection<String> fieldNames) {
		for (Iterator<IssueField> it = fields.iterator(); it.hasNext();) {
			if (fieldNames.contains(it.next().getName()))
				it.remove();
		}
		fieldInputs = null;
	}

	public Map<String, Input> getFieldInputs() {
		if (fieldInputs == null) {
			fieldInputs = new LinkedHashMap<>();
			Map<String, List<IssueField>> fieldMap = new HashMap<>();
			for (IssueField field : fields) {
				List<IssueField> fieldsOfName = fieldMap.get(field.getName());
				if (fieldsOfName == null) {
					fieldsOfName = new ArrayList<>();
					fieldMap.put(field.getName(), fieldsOfName);
				}
				fieldsOfName.add(field);
			}
			for (FieldSpec fieldSpec : getIssueSetting().getFieldSpecs()) {
				String fieldName = fieldSpec.getName();
				List<IssueField> fields = fieldMap.get(fieldName);
				if (fields != null) {
					Collections.sort(fields, new Comparator<IssueField>() {
						@Override
						public int compare(IssueField o1, IssueField o2) {
							long result = o1.getOrdinal() - o2.getOrdinal();
							if (result > 0)
								return 1;
							else if (result < 0)
								return -1;
							else
								return 0;
						}
					});
					String type = fields.iterator().next().getType();
					List<String> values = new ArrayList<>();
					for (IssueField field : fields) {
						if (field.getValue() != null)
							values.add(field.getValue());
					}
					if (!fieldSpec.isAllowMultiple() && values.size() > 1)
						values = Lists.newArrayList(values.iterator().next());
					fieldInputs.put(fieldName, new Input(fieldName, type, values));
				}
			}
		}
		return fieldInputs;
	}

	public void setFieldValue(String fieldName, @Nullable Object fieldValue, Issue issue) {
		for (Iterator<IssueField> it = fields.iterator(); it.hasNext();) {
			if (fieldName.equals(it.next().getName()))
				it.remove();
		}
		FieldSpec fieldSpec = getIssueSetting().getFieldSpec(fieldName);
		if (fieldSpec != null) {
			List<String> strings = fieldSpec.convertToStrings(fieldValue);
			if (!strings.isEmpty()) {
				for (String string : strings) {
					IssueField field = new IssueField();
					field.setIssue(issue);
					field.setName(fieldName);
					field.setOrdinal(getFieldOrdinal(fieldName, string));
					field.setType(fieldSpec.getType());
					field.setValue(string);
					fields.add(field);
				}
			} else {
				IssueField field = new IssueField();
				field.setIssue(issue);
				field.setName(fieldName);
				field.setOrdinal(getFieldOrdinal(fieldName, null));
				field.setType(fieldSpec.getType());
				fields.add(field);
			}
		}
		fieldInputs = null;
	}

	public void setFieldValues(Map<String, Object> fieldValues, Issue issue) {
		for (Map.Entry<String, Object> entry : fieldValues.entrySet())
			setFieldValue(entry.getKey(), entry.getValue(), issue);
	}

	@Nullable
	public Object getFieldValue(String fieldName) {
		Input input = getFieldInputs().get(fieldName);
		if (input != null)
			return input.getTypedValue(getIssueSetting().getFieldSpec(fieldName));
		else
			return null;
	}

	public Serializable getFieldBean(Class<?> fieldBeanClass, boolean withDefaultValue) {
		BeanDescriptor beanDescriptor = new BeanDescriptor(fieldBeanClass);
		Serializable fieldBean = (Serializable) beanDescriptor.newBeanInstance();
		for (List<PropertyDescriptor> groupProperties : beanDescriptor.getProperties().values()) {
			for (PropertyDescriptor property : groupProperties) {
				Input input = getFieldInputs().get(property.getDisplayName());
				if (input != null) {
					FieldSpec fieldSpec = getIssueSetting().getFieldSpec(input.getName());
					property.setPropertyValue(fieldBean, input.getTypedValue(fieldSpec));
				} else if (!withDefaultValue) {
					property.setPropertyValue(fieldBean, null);
				}
			}
		}
		return fieldBean;
	}

	public boolean isFieldVisible(String fieldName, Set<String> checkedFieldNames) {
		if (!checkedFieldNames.add(fieldName))
			return false;
		FieldSpec fieldSpec = getIssueSetting().getFieldSpec(fieldName);
		if (fieldSpec != null) {
			if (fieldSpec.getShowCondition() != null) {
				Input dependentInput = getFieldInputs().get(fieldSpec.getShowCondition().getInputName());
				if (dependentInput != null) {
					if (fieldSpec.getShowCondition().getValueMatcher().matches(dependentInput.getValues()))
						return isFieldVisible(dependentInput.getName(), checkedFieldNames);
					else
						return false;
				} else {
					return false;
				}
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	public GlobalIssueSetting getIssueSetting() {
		return OneDev.getInstance(SettingManager.class).getIssueSetting();
	}

	public long getFieldOrdinal(String fieldName, String fieldValue) {
		GlobalIssueSetting issueSetting = OneDev.getInstance(SettingManager.class).getIssueSetting();
		FieldSpec fieldSpec = issueSetting.getFieldSpec(fieldName);
		if (fieldSpec != null)
			return fieldSpec.getOrdinal(fieldValue);
		else
			return -1;
	}
}
package com.jb.entities.subfields;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberInteger;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;
import com.ccp.json.validations.global.annotations.CcpJsonValidationFieldList;

/**
 * Rules of a {@code stepFlow} item of {@code JbEntityBotCommandStep}: the status is required, and so is at least one of
 * {@code message} and {@code nextStep}.
 */
@CcpJsonGlobalValidations(requiresAtLeastOne = {@CcpJsonValidationFieldList(JbNextStepFields.AtLeastOne.class)})
public enum JbNextStepFields implements CcpJsonFieldName{
	
	/** The {@code message} field: list, nested JSON. */
	@CcpJsonFieldValidatorArray(minSize = 1)
	@CcpJsonFieldTypeNestedJson(jsonValidation = JbNextStepMessageFields.class)
	message,
	
	/** The {@code status} field: required, integer number. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeNumberInteger
	status,
	
	/** The {@code nextStep} field: text. */
	@CcpJsonFieldTypeString
	nextStep,

	;

	/**
	 * Group of the {@code requiresAtLeastOne}. It has to be public: the global validation engine reads the items by
	 * reflection, from another package, and a package-private enum made every validation of {@code JbEntityBotCommandStep}
	 * throw {@code IllegalAccessException}.
	 */
	public static enum AtLeastOne{
		/** The messages. */
		message,
		/** The alternative step. */
		nextStep
	}
}

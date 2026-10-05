package com.jb.entities.subfields;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/** Rules of a message of a {@code stepFlow} item. */
public enum JbNextStepMessageFields implements CcpJsonFieldName{
	/** The {@code language} field: required, validated as in {@code JnJsonCommonsFields}. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	language,
	/** The {@code message} field: required, validated as in {@code JnJsonCommonsFields}. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	message
	;
}

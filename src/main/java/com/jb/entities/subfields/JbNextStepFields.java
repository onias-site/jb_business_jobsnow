package com.jb.entities.subfields;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberInteger;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;
import com.ccp.json.validations.global.annotations.CcpJsonValidationFieldList;

@CcpJsonGlobalValidations(requiresAtLeastOne = {@CcpJsonValidationFieldList(JbNextStepFields.AtLeastOne.class)})
public enum JbNextStepFields implements CcpJsonFieldName{
	
	@CcpJsonFieldValidatorArray(minSize = 1)
	@CcpJsonFieldTypeNestedJson(jsonValidation = JbNextStepMessageFields.class)
	message,
	
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeNumberInteger
	status,
	
	@CcpJsonFieldTypeString
	nextStep,

	;

	/**
	 * Grupo do {@code requiresAtLeastOne}. Tem que ser público: o motor de validação global lê os itens
	 * por reflexão, de outro pacote, e um enum com visibilidade de pacote faz toda validação de
	 * {@code JbEntityBotCommandStep} estourar {@code IllegalAccessException}.
	 */
	public static enum AtLeastOne{
		message, nextStep
	}
}

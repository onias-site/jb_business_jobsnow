package com.jb.business.bots.login.token;

import static com.jb.business.bots.login.token.LoginTokenTicketsJsonTransformers.readAllLoginTokenTicketsFunction;

import java.util.List;
import java.util.function.Predicate;

import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.utils.CcpDbRequester;
import com.jn.json.fields.validation.JnJsonCommonsFields;

import com.ccp.decorators.CcpStringDecorator;/**
 * Predicados reutilizáveis para avaliar condições sobre o JSON de tickets de token de login.
 * {@code hasAlegation} verifica se há alegação no idioma corrente; {@code ifIsTheLastLoginTokenTicket}
 * verifica se a posição atual é o último ticket da lista.
 */

public enum LoginTokenTicketsJsonConditions implements Predicate<CcpJsonRepresentation>{

	hasAlegation{

		public boolean test(CcpJsonRepresentation json) {

			String alegation = getAlegation(json);
			String alegationTrim = alegation.trim();
			boolean alegationTrimEmpty = alegationTrim.isEmpty();

			boolean hasAlegation = false == alegationTrimEmpty;
			
			return hasAlegation;
		}
	},
	
	
	ifIsTheLastLoginTokenTicket{

		public boolean test(CcpJsonRepresentation json) {
			LoginTokenTicketsJsonFields[] loginTokenTicketsJsonFieldsValues = LoginTokenTicketsJsonFields.values();
			CcpJsonRepresentation result = json.whenAllFieldsAreFound(readAllLoginTokenTicketsFunction, loginTokenTicketsJsonFieldsValues);
			List<CcpJsonRepresentation> listValues = result.getAsJsonList(LoginTokenTicketsJsonFields.listValues);
			Integer counter = result.getAsIntegerNumber(LoginTokenTicketsJsonFields.counter);
			int position = counter + 1;
			int listValuesSize = listValues.size();

			boolean isTheLastLoginTokenTicket = position == listValuesSize;
			
			return isTheLastLoginTokenTicket;
		}
	};

	static String getAlegation(CcpJsonRepresentation json) {
		String fieldNameToEntity = CcpDependencyInjection.getDependency(CcpDbRequester.class).getFieldNameToEntity();
	
		CcpJsonFieldName entityName = new CcpFieldName(fieldNameToEntity);
		CcpStringDecorator asStringDecorator = json.getAsStringDecorator(JnJsonCommonsFields.language);

		CcpJsonFieldName language = asStringDecorator.jsonFieldName();
		
		String alegation = json.getValueFromPath("", language, entityName);
		
		return alegation;
	}
	
	
}

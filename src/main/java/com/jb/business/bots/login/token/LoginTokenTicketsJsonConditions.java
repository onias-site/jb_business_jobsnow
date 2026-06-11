package com.jb.business.bots.login.token;

import static com.jb.business.bots.login.token.LoginTokenTicketsJsonTransformers.readAllLoginTokenTicketsFunction;

import java.util.List;
import java.util.function.Predicate;

import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.utils.CcpDbRequester;
import com.jb.entities.JbEntityBotExplanation;
public enum LoginTokenTicketsJsonConditions implements Predicate<CcpJsonRepresentation>{

	hasAlegation{

		public boolean test(CcpJsonRepresentation json) {

			String alegation = getAlegation(json);
			
			boolean hasAlegation = false == alegation.trim().isEmpty();
			
			return hasAlegation;
		}
	},
	
	
	ifIsTheLastLoginTokenTicket{

		public boolean test(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = json.whenAllFieldsAreFound(readAllLoginTokenTicketsFunction, LoginTokenTicketsJsonFields.values());
			List<CcpJsonRepresentation> listValues = result.getAsJsonList(LoginTokenTicketsJsonFields.listValues);
			Integer counter = result.getAsIntegerNumber(LoginTokenTicketsJsonFields.counter);
			int position = counter + 1;

			boolean isTheLastLoginTokenTicket = position == listValues.size();
			
			return isTheLastLoginTokenTicket;
		}
	};

	static String getAlegation(CcpJsonRepresentation json) {
		
		CcpJsonFieldName entityName = new CcpFieldName(CcpDependencyInjection.getDependency(CcpDbRequester.class).getFieldNameToEntity());

		CcpJsonFieldName language = json.getAsStringDecorator(JbEntityBotExplanation.Fields.language).jsonFieldName();
		
		String alegation = json.getValueFromPath("", language, entityName);
		
		return alegation;
	}
	
	
}

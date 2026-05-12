package com.jb.business.bots.login.token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ccp.business.CcpBusiness;
import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpDynamicJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.query.CcpQueryExecutorDecorator;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.utils.CcpDbRequester;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.entities.JnEntityLoginTokenRequestResend;
import com.jn.entities.JnEntityLoginTokenRequestUnlock;

enum LoginTokenTicketsJsonTransformers implements CcpBusiness{
	readAllLoginTokenTicketsFunction{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpQueryOptions query = CcpQueryOptions.INSTANCE.matchAll();
			CcpQueryExecutorDecorator selectFrom = query.selectFrom(JnEntityLoginTokenRequestResend.ENTITY, JnEntityLoginTokenRequestUnlock.ENTITY);
			CcpDbRequester dependency = CcpDependencyInjection.getDependency(CcpDbRequester.class);
			String fieldNameToEntity = dependency.getFieldNameToEntity();
			
			List<CcpJsonRepresentation> resultAsList = selectFrom.getResultAsList(
					JnEntityLoginTokenRequestResend.Fields.email.name(),
					fieldNameToEntity
					);
			
			boolean hasNoLoginTokenTicket = resultAsList.isEmpty();
			
			if(hasNoLoginTokenTicket) {
				throw new CcpErrorFlowDisturb(CcpOtherConstants.EMPTY_JSON, CcpProcessStatusDefault.NOT_FOUND);
			}
			
			int listSize = resultAsList.size();
			int counter = json.getOrDefault(LoginTokenTicketsJsonFields.counter, () -> 0);
			CcpJsonRepresentation newJson = CcpOtherConstants.EMPTY_JSON
			.put(LoginTokenTicketsJsonFields.listValues, resultAsList)
			.put(LoginTokenTicketsJsonFields.listSize, listSize)
			.put(LoginTokenTicketsJsonFields.counter, counter)
			;
			return newJson;
		}

	},
	solveLoginTokenTicketsFunction{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = json.whenAllFieldsAreFound(readAllLoginTokenTicketsFunction, LoginTokenTicketsJsonFields.values());
			List<CcpJsonRepresentation> listValues = result.getAsJsonList(LoginTokenTicketsJsonFields.listValues);
			Integer counter = result.getAsIntegerNumber(LoginTokenTicketsJsonFields.counter);
			int position = counter + 1;
			CcpDbRequester dependency = CcpDependencyInjection.getDependency(CcpDbRequester.class);
			String fieldNameToEntity = dependency.getFieldNameToEntity();
			CcpJsonRepresentation ticket = listValues.get(counter);
			CcpJsonRepresentation jsonWithTicket = json.mergeWithAnotherJson(ticket);
			CcpDynamicJsonRepresentation dynamicVersion = jsonWithTicket.getDynamicVersion();
			String entityName = dynamicVersion.getAsString(fieldNameToEntity);
			CcpEntity entity = entities.get(entityName);
			CcpJsonRepresentation allDataAboutTicketSolution = entity.delete(jsonWithTicket);
			CcpJsonRepresentation ticketWithAllDataAboutTicketSolution = allDataAboutTicketSolution.mergeWithAnotherJson(allDataAboutTicketSolution);
			
			CcpJsonRepresentation updatedValues = ticketWithAllDataAboutTicketSolution
			.put(LoginTokenTicketsJsonFields.counter, counter + 1)
			.put(LoginTokenTicketsJsonFields.position, position);
			
			return updatedValues;

		}
	},
	
	throwNewHasNoMoreLoginTokenTicketsToSolve{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation throwException = CcpProcessStatusDefault.NOT_FOUND.throwException(CcpOtherConstants.EMPTY_JSON);
			return throwException;
		}
	}
	;
	static Map<String, CcpEntity> entities = new HashMap<>();
	
	static {
		entities.put(JnEntityLoginTokenRequestResend.ENTITY.getEntityMetaData().entityName, JnEntityLoginTokenRequestResend.ENTITY);
		entities.put(JnEntityLoginTokenRequestUnlock.ENTITY.getEntityMetaData().entityName, JnEntityLoginTokenRequestUnlock.ENTITY);
	}

}

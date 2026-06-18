package com.jb.business.bots.login.token;

import static com.jb.business.bots.login.token.LoginTokenTicketsJsonConditions.hasAlegation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpGetEntityId.CcpSelectUnionAll;
import com.ccp.especifications.db.query.CcpQueryOptions.CcpQueryExecutorDecorator;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.utils.CcpDbRequester;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.entities.JnEntityLoginTokenRequestResend;
import com.jn.entities.JnEntityLoginTokenRequestUnlock;
import com.jn.entities.JnEntitySystemMessage;
import com.jn.utils.JnDeleteKeysFromCache;

/**
 * Transformadores de JSON que encapsulam as operações de leitura, seleção e resolução de
 * tickets de token de login pendentes (Unlock e Resend). Inclui {@code readAllLoginTokenTicketsFunction},
 * {@code solveLoginTokenTicketsFunction}, {@code searchAlegations}, {@code chooseOneAlegation} e
 * {@code throwNewHasNoMoreLoginTokenTicketsToSolve}.
 */
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
	
	searchAlegations{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

			CcpJsonFieldName entityName = new CcpFieldName(CcpDependencyInjection.getDependency(CcpDbRequester.class).getFieldNameToEntity());

			Supplier<CcpJsonRepresentation> jsonSupplier = () -> json.duplicateValueFromField(entityName, JnEntitySystemMessage.Fields.systemMessageName);

			CcpJsonRepresentation parametersToSearch = jsonSupplier.get();
			
			CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
			
			CcpSelectUnionAll resultFromSearchBots = crud.unionAll(parametersToSearch, JnDeleteKeysFromCache.INSTANCE, JnEntitySystemMessage.ENTITY);
			
			CcpJsonRepresentation systemMessage = JnEntitySystemMessage.ENTITY.getRecordFromUnionAll(resultFromSearchBots, jsonSupplier);
			
			String alegation = systemMessage.getAsString(JnEntitySystemMessage.Fields.message);

			CcpJsonRepresentation put = json.put(OtherFields.alegation, alegation);
			
			return put;
		}
	},
	
	chooseOneAlegation{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			String alegation = LoginTokenTicketsJsonConditions.getAlegation(json);
			CcpJsonRepresentation put = json.put(OtherFields.alegation, alegation);
			return put;
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
			String entityName = jsonWithTicket.getAsString(new CcpFieldName(fieldNameToEntity));
			CcpEntity entity = entities.get(entityName);
			CcpJsonRepresentation allDataAboutTicketSolution = entity.delete(jsonWithTicket);
			CcpJsonRepresentation ticketWithAllDataAboutTicketSolution = allDataAboutTicketSolution.mergeWithAnotherJson(allDataAboutTicketSolution);

			CcpJsonRepresentation ticketWithAlegation = ticketWithAllDataAboutTicketSolution
					.getTransformedJsonExecutingIfAndElse(hasAlegation, CcpOtherConstants.DO_NOTHING, searchAlegations)
					.getTransformedJson(chooseOneAlegation)
					;
			
			CcpJsonRepresentation updatedValues = ticketWithAlegation
			.put(LoginTokenTicketsJsonFields.counter, counter + 1)
			.put(LoginTokenTicketsJsonFields.position, position)
			;
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

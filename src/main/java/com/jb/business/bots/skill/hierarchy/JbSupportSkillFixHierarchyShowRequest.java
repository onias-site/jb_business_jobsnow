package com.jb.business.bots.skill.hierarchy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisSkillFixHierarchyReviewFields;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

/**
 * First step of the {@code fixSkillHierarchy} command ({@code /fixSkillHierarchy <parent> <email>}): shows the
 * operator the request of the user for that parent, with the justification the user gave ({@code description})
 * and the items still pending, and asks how to decide them. Both types of request ({@code add} and
 * {@code remove}) for the same parent are shown together.
 *
 * <p>Only the items still in {@link VisEntitySkillFixHierarchyItemPending} are shown: the {@code skill} of the
 * request may list skills already approved or rejected before. Without any pending item the flow is diverted
 * with {@code requestNotFound}.
 */
public class JbSupportSkillFixHierarchyShowRequest implements CcpBusiness {

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		CcpJsonRepresentation requestKey = json.getJsonPiece(VisEntitySkillFixHierarchyPending.Fields.email, VisEntitySkillFixHierarchyPending.Fields.parent);
		VisSkillFixHierarchyTypes[] types = VisSkillFixHierarchyTypes.values();
		List<CcpJsonRepresentation> requestKeys = new ArrayList<>();

		for (VisSkillFixHierarchyTypes type : types) {
			CcpJsonRepresentation requestKeyWithType = requestKey.put(VisEntitySkillFixHierarchyPending.Fields.type, type);
			requestKeys.add(requestKeyWithType);
		}

		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpJsonRepresentation[] requestKeysArray = requestKeys.toArray(new CcpJsonRepresentation[requestKeys.size()]);
		CcpSelectUnionAll requests = crud.unionAll(requestKeysArray, JnDeleteKeysFromCache.INSTANCE, VisEntitySkillFixHierarchyPending.ENTITY);

		List<CcpJsonRepresentation> foundRequests = new ArrayList<>();
		List<CcpJsonRepresentation> candidateItems = new ArrayList<>();

		for (CcpJsonRepresentation requestKeyWithType : requestKeys) {
			Supplier<CcpJsonRepresentation> requestKeySupplier = requestKeyWithType.getJsonSupplier();
			CcpJsonRepresentation request = VisEntitySkillFixHierarchyPending.ENTITY.getRecordFromUnionAll(requests, requestKeySupplier);

			boolean requestNotFound = request.isEmpty();

			if(requestNotFound) {
				continue;
			}

			foundRequests.add(request);
			List<String> skills = request.getAsStringList(VisEntitySkillFixHierarchyPending.Fields.skill);

			for (String skill : skills) {
				CcpJsonRepresentation candidateItem = requestKeyWithType.put(VisEntitySkillFixHierarchyItemPending.Fields.skill, skill);
				candidateItems.add(candidateItem);
			}
		}

		boolean noCandidateItem = candidateItems.isEmpty();

		if(noCandidateItem) {
			JbSupportSkillFixHierarchyStatus.requestNotFound.throwException(json);
		}

		CcpJsonRepresentation[] candidateItemsArray = candidateItems.toArray(new CcpJsonRepresentation[candidateItems.size()]);
		CcpSelectUnionAll items = crud.unionAll(candidateItemsArray, JnDeleteKeysFromCache.INSTANCE, VisEntitySkillFixHierarchyItemPending.ENTITY);
		List<CcpJsonRepresentation> reviewItems = new ArrayList<>();

		for (CcpJsonRepresentation candidateItem : candidateItems) {
			boolean isPending = VisEntitySkillFixHierarchyItemPending.ENTITY.isPresentInThisUnionAll(items, candidateItem);

			if(false == isPending) {
				continue;
			}

			CcpJsonRepresentation reviewItem = candidateItem.getJsonPiece(VisEntitySkillFixHierarchyItemPending.Fields.type, VisEntitySkillFixHierarchyItemPending.Fields.skill);
			reviewItems.add(reviewItem);
		}

		boolean noPendingItem = reviewItems.isEmpty();

		if(noPendingItem) {
			JbSupportSkillFixHierarchyStatus.requestNotFound.throwException(json);
		}

		String requestText = this.getRequestText(json, foundRequests, reviewItems);

		CcpJsonRepresentation jsonWithItems = json.put(JbSupportSkillFixHierarchyFields.reviewItems, reviewItems);
		CcpJsonRepresentation jsonWithIndex = jsonWithItems.put(JbSupportSkillFixHierarchyFields.itemIndex, 0);
		CcpJsonRepresentation jsonWithoutDecisions = jsonWithIndex.put(VisSkillFixHierarchyReviewFields.reviewDecisions, new ArrayList<>());
		CcpJsonRepresentation jsonWithReply = jsonWithoutDecisions.put(JbSupportSkillFixHierarchyFields.botReply, requestText);
		return jsonWithReply;
	}

	private String getRequestText(CcpJsonRepresentation json, List<CcpJsonRepresentation> foundRequests, List<CcpJsonRepresentation> reviewItems) {

		JnLanguage language = JbSupportSkillFixHierarchyConversation.getLanguage(json);
		boolean portuguese = JbSupportSkillFixHierarchyConversation.isPortuguese(language);
		String email = json.getAsString(VisEntitySkillFixHierarchyPending.Fields.email);
		String parent = json.getAsString(VisEntitySkillFixHierarchyPending.Fields.parent);

		StringBuilder requestText = new StringBuilder();
		String header = portuguese
				? "Solicitação de " + email + " para o termo " + parent + "\n\n"
				: "Request from " + email + " for the term " + parent + "\n\n";
		requestText.append(header);

		for (CcpJsonRepresentation request : foundRequests) {
			VisSkillFixHierarchyTypes type = request.getAsEnum(VisEntitySkillFixHierarchyPending.Fields.type, VisSkillFixHierarchyTypes.class);
			String typeName = type.name();
			String typeDescription = type.getDescription(language);
			String description = request.getAsString(VisEntitySkillFixHierarchyPending.Fields.description);

			List<String> pendingSkills = new ArrayList<>();

			for (CcpJsonRepresentation reviewItem : reviewItems) {
				String itemType = reviewItem.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.type);
				boolean isAnotherType = false == typeName.equals(itemType);

				if(isAnotherType) {
					continue;
				}

				String skill = reviewItem.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill);
				pendingSkills.add(skill);
			}

			boolean noPendingSkillOfThisType = pendingSkills.isEmpty();

			if(noPendingSkillOfThisType) {
				continue;
			}

			String joinedSkills = String.join(", ", pendingSkills);
			String typeText = portuguese
					? "[" + typeDescription + "]\nJustificativa do usuário: " + description + "\nItens pendentes: " + joinedSkills + "\n\n"
					: "[" + typeDescription + "]\nUser's justification: " + description + "\nPending items: " + joinedSkills + "\n\n";
			requestText.append(typeText);
		}

		String options = JbSupportSkillFixHierarchyConversation.getOptions(language);
		requestText.append(options);
		String text = requestText.toString();
		return text;
	}
}

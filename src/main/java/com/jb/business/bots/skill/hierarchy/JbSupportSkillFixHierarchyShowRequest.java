package com.jb.business.bots.skill.hierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisBusinessSkillFixHierarchyReview;
import com.vis.business.skill.VisSkillFixHierarchyDecisions;
import com.vis.business.skill.VisSkillFixHierarchyReviewFields;
import com.vis.entities.VisEntityCommandNotAllowedToUser;
import com.vis.entities.VisEntitySkillFixHierarchyItemApproved;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes; 
import com.vis.json.fields.validation.VisUserRequestCommands;

/**
 * First step of the {@code fixSkillHierarchy} command ({@code /fixSkillHierarchy <parent> <type> <email>}): shows the
 * operator the request of the user for that parent, with the justification the user gave ({@code description})
 * and the items still pending, and asks how to decide them. Only the request of the {@code type} given in the
 * command ({@code add} or {@code remove}) is shown: the user associates and dissociates through different
 * buttons, each one notifies the operator with its own command, and up to 2026-09-30 the command had no type,
 * so the notice of a dissociation was indistinguishable from the one of an association. A {@code type} that is
 * neither of them has no request to show ({@code requestNotFound}).
 *
 * <p>The {@code skill} of the request may list skills decided in earlier reviews. Those found in the rejected
 * items (the twin of {@link VisEntitySkillFixHierarchyItemPending}) or in
 * {@link VisEntitySkillFixHierarchyItemApproved} are not asked to the operator: they enter the
 * {@code reviewDecisions} already decided, as rejected or approved, and so reach the user's feedback. Only the
 * items still in {@link VisEntitySkillFixHierarchyItemPending} are asked. When every item was decided before,
 * the review finishes right here ({@code reviewFinished}); without any item at all the flow is diverted with
 * {@code requestNotFound}.
 *
 * <p>A user ignored for the command ({@link VisEntityCommandNotAllowedToUser}) is not reviewed, even if the
 * operator runs the command for them by mistake or on purpose: the flow is diverted with {@code userNotAllowed}
 * before anything is read, and the operator is told how to stop ignoring the user.
 */
public class JbSupportSkillFixHierarchyShowRequest implements CcpBusiness {

	/**
	 * Shows the request and its pending items.
	 * @param json the session, with {@code parent}, {@code type} and {@code email}
	 * @return the session with the items to review, the earlier decisions and the reply
	 * @throws CcpErrorFlowDisturb with {@code userNotAllowed}, {@code requestNotFound} or {@code reviewFinished}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		String email = json.getAsString(VisEntitySkillFixHierarchyPending.Fields.email);
		CcpJsonRepresentation ignoredUserWithEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityCommandNotAllowedToUser.Fields.email, email);
		CcpJsonRepresentation ignoredUser = ignoredUserWithEmail.put(VisEntityCommandNotAllowedToUser.Fields.commandName, VisUserRequestCommands.fixSkillHierarchy);
		boolean userIsIgnored = VisEntityCommandNotAllowedToUser.ENTITY.exists(ignoredUser);

		if(userIsIgnored) {
			JbSupportSkillFixHierarchyStatus.userNotAllowed.throwException(json);
		}

		String typeName = json.getAsString(VisEntitySkillFixHierarchyPending.Fields.type);
		Stream<VisSkillFixHierarchyTypes> typesStream = Arrays.stream(VisSkillFixHierarchyTypes.values());
		boolean unknownType = typesStream.noneMatch(type -> type.name().equals(typeName));

		if(unknownType) {
			JbSupportSkillFixHierarchyStatus.requestNotFound.throwException(json);
		}

		CcpJsonRepresentation requestKeyOfTheType = json.getJsonPiece(VisEntitySkillFixHierarchyPending.Fields.email, VisEntitySkillFixHierarchyPending.Fields.parent, VisEntitySkillFixHierarchyPending.Fields.type);
		List<CcpJsonRepresentation> requestKeys = Arrays.asList(requestKeyOfTheType);

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

		CcpEntity rejectedItemEntity = VisEntitySkillFixHierarchyItemPending.ENTITY.getTwinEntity();
		CcpJsonRepresentation[] candidateItemsArray = candidateItems.toArray(new CcpJsonRepresentation[candidateItems.size()]);
		CcpSelectUnionAll items = crud.unionAll(candidateItemsArray, JnDeleteKeysFromCache.INSTANCE, VisEntitySkillFixHierarchyItemPending.ENTITY, rejectedItemEntity, VisEntitySkillFixHierarchyItemApproved.ENTITY);
		List<CcpJsonRepresentation> reviewItems = new ArrayList<>();
		List<CcpJsonRepresentation> previousDecisions = new ArrayList<>();
		JnLanguage language = JbSupportSkillFixHierarchyConversation.getLanguage(json);

		for (CcpJsonRepresentation candidateItem : candidateItems) {
			String type = candidateItem.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.type);
			String skill = candidateItem.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill);

			boolean wasRejectedBefore = rejectedItemEntity.isPresentInThisUnionAll(items, candidateItem);

			if(wasRejectedBefore) {
				String justification = JbSupportSkillFixHierarchyConversation.getPreviousDecisionJustification(language, VisSkillFixHierarchyDecisions.rejected);
				CcpJsonRepresentation previousDecision = VisBusinessSkillFixHierarchyReview.getDecision(type, skill, VisSkillFixHierarchyDecisions.rejected, justification);
				previousDecisions.add(previousDecision);
				continue;
			}

			boolean wasApprovedBefore = VisEntitySkillFixHierarchyItemApproved.ENTITY.isPresentInThisUnionAll(items, candidateItem);

			if(wasApprovedBefore) {
				String justification = JbSupportSkillFixHierarchyConversation.getPreviousDecisionJustification(language, VisSkillFixHierarchyDecisions.approved);
				CcpJsonRepresentation previousDecision = VisBusinessSkillFixHierarchyReview.getDecision(type, skill, VisSkillFixHierarchyDecisions.approved, justification);
				previousDecisions.add(previousDecision);
				continue;
			}

			boolean isPending = VisEntitySkillFixHierarchyItemPending.ENTITY.isPresentInThisUnionAll(items, candidateItem);

			if(false == isPending) {
				continue;
			}

			CcpJsonRepresentation reviewItem = candidateItem.getJsonPiece(VisEntitySkillFixHierarchyItemPending.Fields.type, VisEntitySkillFixHierarchyItemPending.Fields.skill);
			reviewItems.add(reviewItem);
		}

		boolean noPendingItem = reviewItems.isEmpty();
		boolean noPreviousDecision = previousDecisions.isEmpty();

		if(noPendingItem && noPreviousDecision) {
			JbSupportSkillFixHierarchyStatus.requestNotFound.throwException(json);
		}

		CcpJsonRepresentation jsonWithItems = json.put(JbSupportSkillFixHierarchyFields.reviewItems, reviewItems);
		CcpJsonRepresentation jsonWithIndex = jsonWithItems.put(JbSupportSkillFixHierarchyFields.itemIndex, 0);

		// every item was already decided in earlier reviews: there is nothing to ask the operator
		if(noPendingItem) {
			CcpJsonRepresentation finished = JbSupportSkillFixHierarchyConversation.finish(jsonWithIndex, previousDecisions);
			return finished;
		}

		String requestText = this.getRequestText(json, foundRequests, reviewItems, previousDecisions);

		CcpJsonRepresentation jsonWithPreviousDecisions = jsonWithIndex.put(VisSkillFixHierarchyReviewFields.reviewDecisions, previousDecisions);
		CcpJsonRepresentation jsonWithReply = jsonWithPreviousDecisions.put(JbSupportSkillFixHierarchyFields.botReply, requestText);
		return jsonWithReply;
	}

	/**
	 * Builds the text of the request: a header, then for each type the justification of the user, the pending items and the
	 * items decided before, and finally the options (see finding: the texts are literals, not system messages).
	 * @param json the session
	 * @param foundRequests the pending requests
	 * @param reviewItems the items to review
	 * @param previousDecisions the items decided before
	 * @return the text
	 */
	private String getRequestText(CcpJsonRepresentation json, List<CcpJsonRepresentation> foundRequests, List<CcpJsonRepresentation> reviewItems, List<CcpJsonRepresentation> previousDecisions) {

		JnLanguage language = JbSupportSkillFixHierarchyConversation.getLanguage(json);
		boolean portuguese = JbSupportSkillFixHierarchyConversation.isPortuguese(language);
		String email = json.getAsString(VisEntitySkillFixHierarchyPending.Fields.email);
		String parent = json.getAsString(VisEntitySkillFixHierarchyPending.Fields.parent);
		VisSkillFixHierarchyTypes requestType = json.getAsEnum(VisEntitySkillFixHierarchyPending.Fields.type, VisSkillFixHierarchyTypes.class);
		String requestTypeDescription = requestType.getDescription(language);

		StringBuilder requestText = new StringBuilder();
		String header = portuguese
				? "Solicitação de " + requestTypeDescription + " de " + email + " para o termo " + parent + "\n\n"
				: "Request of " + requestTypeDescription + " from " + email + " for the term " + parent + "\n\n";
		requestText.append(header);

		for (CcpJsonRepresentation request : foundRequests) {
			VisSkillFixHierarchyTypes type = request.getAsEnum(VisEntitySkillFixHierarchyPending.Fields.type, VisSkillFixHierarchyTypes.class);
			String typeName = type.name();
			String typeDescription = type.getDescription(language);
			String description = request.getAsString(VisEntitySkillFixHierarchyPending.Fields.description);

			List<String> pendingSkills = this.getSkillsOfTheType(reviewItems, typeName);
			List<String> approvedBeforeSkills = this.getSkillsOfTheTypeWithTheDecision(previousDecisions, typeName, VisSkillFixHierarchyDecisions.approved);
			List<String> rejectedBeforeSkills = this.getSkillsOfTheTypeWithTheDecision(previousDecisions, typeName, VisSkillFixHierarchyDecisions.rejected);

			boolean hasPendingSkill = false == pendingSkills.isEmpty();
			boolean hasApprovedBefore = false == approvedBeforeSkills.isEmpty();
			boolean hasRejectedBefore = false == rejectedBeforeSkills.isEmpty();
			boolean nothingOfThisType = false == hasPendingSkill && false == hasApprovedBefore && false == hasRejectedBefore;

			if(nothingOfThisType) {
				continue;
			}

			String typeText = portuguese
					? "[" + typeDescription + "]\nJustificativa do usuário: " + description + "\n"
					: "[" + typeDescription + "]\nUser's justification: " + description + "\n";
			requestText.append(typeText);

			if(hasPendingSkill) {
				String joinedSkills = String.join(", ", pendingSkills);
				String pendingText = portuguese
						? "Itens pendentes: " + joinedSkills + "\n"
						: "Pending items: " + joinedSkills + "\n";
				requestText.append(pendingText);
			}

			if(hasApprovedBefore) {
				String joinedApprovedBefore = String.join(", ", approvedBeforeSkills);
				String approvedBeforeText = portuguese
						? "Já aprovados anteriormente (não serão perguntados): " + joinedApprovedBefore + "\n"
						: "Already approved before (will not be asked): " + joinedApprovedBefore + "\n";
				requestText.append(approvedBeforeText);
			}

			if(hasRejectedBefore) {
				String joinedRejectedBefore = String.join(", ", rejectedBeforeSkills);
				String rejectedBeforeText = portuguese
						? "Já reprovados anteriormente (não serão perguntados): " + joinedRejectedBefore + "\n"
						: "Already rejected before (will not be asked): " + joinedRejectedBefore + "\n";
				requestText.append(rejectedBeforeText);
			}

			requestText.append("\n");
		}

		String options = JbSupportSkillFixHierarchyConversation.getOptions(language);
		requestText.append(options);
		String text = requestText.toString();
		return text;
	}

	/**
	 * Returns the skills of the items of the type.
	 * @param items the items
	 * @param typeName the type
	 * @return the skills
	 */
	private List<String> getSkillsOfTheType(List<CcpJsonRepresentation> items, String typeName) {
		Stream<CcpJsonRepresentation> itemsStream = items.stream();
		Stream<CcpJsonRepresentation> itemsOfTheTypeStream = itemsStream.filter(item -> typeName.equals(item.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.type)));
		Stream<String> skillsStream = itemsOfTheTypeStream.map(item -> item.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill));
		List<String> skills = skillsStream.collect(Collectors.toList());
		return skills;
	}

	/**
	 * Returns the skills of the items of the type with the decision.
	 * @param decisions the decisions
	 * @param typeName the type
	 * @param decision the decision
	 * @return the skills
	 */
	private List<String> getSkillsOfTheTypeWithTheDecision(List<CcpJsonRepresentation> decisions, String typeName, VisSkillFixHierarchyDecisions decision) {
		String decisionName = decision.name();
		Stream<CcpJsonRepresentation> decisionsStream = decisions.stream();
		Stream<CcpJsonRepresentation> withTheDecisionStream = decisionsStream.filter(item -> decisionName.equals(item.getAsString(VisSkillFixHierarchyReviewFields.decision)));
		List<CcpJsonRepresentation> withTheDecision = withTheDecisionStream.collect(Collectors.toList());
		List<String> skills = this.getSkillsOfTheType(withTheDecision, typeName);
		return skills;
	}
}

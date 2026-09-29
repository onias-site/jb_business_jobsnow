package com.jb.business.bots.skill.hierarchy;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisBusinessSkillFixHierarchyReview;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;

/**
 * Second step of the {@code fixSkillHierarchy} command: the operator approves all the items, rejects all the
 * items (both with one justification, that goes to every item) or chooses to decide them one by one.
 *
 * <p>Deciding all the items finishes the review ({@code reviewFinished}). Choosing one by one asks for the
 * first item and goes on to the next step. Any other answer, including a decision without justification, is
 * asked again ({@code invalidAnswer}).
 */
public class JbSupportSkillFixHierarchyChooseMode implements CcpBusiness {

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		JbSupportSkillFixHierarchyAnswer answer = JbSupportSkillFixHierarchyAnswer.read(json);

		if(answer.isOneByOne()) {
			String firstItemPrompt = JbSupportSkillFixHierarchyConversation.getItemPrompt(json, 0);
			CcpJsonRepresentation jsonWithPrompt = json.put(JbSupportSkillFixHierarchyFields.botReply, firstItemPrompt);
			return jsonWithPrompt;
		}

		boolean decisionWithJustification = answer.isDecisionWithJustification();

		if(decisionWithJustification) {
			List<CcpJsonRepresentation> reviewItems = json.getAsJsonList(JbSupportSkillFixHierarchyFields.reviewItems);
			List<CcpJsonRepresentation> decisions = new ArrayList<>();

			for (CcpJsonRepresentation reviewItem : reviewItems) {
				String type = reviewItem.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.type);
				String skill = reviewItem.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill);
				CcpJsonRepresentation decision = VisBusinessSkillFixHierarchyReview.getDecision(type, skill, answer.getDecision(), answer.justification);
				decisions.add(decision);
			}

			CcpJsonRepresentation finished = JbSupportSkillFixHierarchyConversation.finish(json, decisions);
			return finished;
		}

		JnLanguage language = JbSupportSkillFixHierarchyConversation.getLanguage(json);
		String notUnderstood = JbSupportSkillFixHierarchyConversation.getNotUnderstood(language);
		String options = JbSupportSkillFixHierarchyConversation.getOptions(language);
		String askAgain = notUnderstood + options;
		CcpJsonRepresentation jsonAskingAgain = json.put(JbSupportSkillFixHierarchyFields.botReply, askAgain);
		CcpJsonRepresentation askedAgain = JbSupportSkillFixHierarchyStatus.invalidAnswer.throwException(jsonAskingAgain);
		return askedAgain;
	}
}

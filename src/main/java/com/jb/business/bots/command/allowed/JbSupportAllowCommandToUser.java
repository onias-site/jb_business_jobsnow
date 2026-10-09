package com.jb.business.bots.command.allowed;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityCommandNotAllowedToUser;

/**
 * Single step of the {@code allowCommandToUser} command ({@code /allowCommandToUser <email>}): undoes
 * the decision of ignoring the user, removing the user from
 * {@link VisEntityCommandNotAllowedToUser}, so that the next requests of the user reach the operator again. The
 * delete moves the record to the twin, vis_command_reallowed_to_user, which keeps it for control and tracking.
 * Without a record for the email the flow is diverted with {@code userNotIgnored}. The ignoring is global, so the
 * command has no other parameter (until 2026-10-08 it was {@code <command> <email>}).
 */
public class JbSupportAllowCommandToUser implements CcpBusiness {

	/**
	 * Deletes the record of the ignored user.
	 * @param json the session, with {@code email}
	 * @return the session
	 * @throws CcpErrorFlowDisturb with {@code userNotIgnored} when there is no such record
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		String email = json.getAsString(JnJsonCommonsFields.email);
		CcpJsonRepresentation ignoredUser = CcpOtherConstants.EMPTY_JSON.put(VisEntityCommandNotAllowedToUser.Fields.email, email);

		boolean deleted = VisEntityCommandNotAllowedToUser.ENTITY.delete(ignoredUser);

		if(false == deleted) {
			CcpJsonRepresentation userNotIgnored = JbSupportAllowCommandToUserStatus.userNotIgnored.throwException(json);
			return userNotIgnored;
		}

		return json;
	}
}

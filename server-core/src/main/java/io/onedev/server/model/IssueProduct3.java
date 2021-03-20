package io.onedev.server.model;


import javax.persistence.OneToMany;
import javax.persistence.CascadeType;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import io.onedev.server.entitymanager.UserManager;
import io.onedev.server.OneDev;
import io.onedev.server.model.support.inputspec.InputSpec;
import io.onedev.server.entitymanager.GroupManager;
import java.io.Serializable;

public class IssueProduct3 implements Serializable {
	private Collection<IssueComment> comments = new ArrayList<>();
	private Collection<IssueChange> changes = new ArrayList<>();
	private transient Collection<User> participants;

	public Collection<IssueComment> getComments() {
		return comments;
	}

	public void setComments(Collection<IssueComment> comments) {
		this.comments = comments;
	}

	public Collection<IssueChange> getChanges() {
		return changes;
	}

	public void setChanges(Collection<IssueChange> changes) {
		this.changes = changes;
	}

	public Collection<User> getParticipants(User thisSubmitter, Issue issue) {
		if (participants == null) {
			participants = new LinkedHashSet<>();
			if (thisSubmitter != null)
				participants.add(thisSubmitter);
			UserManager userManager = OneDev.getInstance(UserManager.class);
			for (IssueField field : issue.getFields()) {
				if (field.getType().equals(InputSpec.USER)) {
					if (field.getValue() != null) {
						User user = userManager.findByName(field.getValue());
						if (user != null)
							participants.add(user);
					}
				} else if (field.getType().equals(InputSpec.GROUP)) {
					if (field.getValue() != null) {
						Group group = OneDev.getInstance(GroupManager.class).find(field.getValue());
						if (group != null)
							participants.addAll(group.getMembers());
					}
				}
			}
			for (IssueComment comment : comments) {
				if (comment.getUser() != null)
					participants.add(comment.getUser());
			}
			for (IssueChange change : changes) {
				if (change.getUser() != null)
					participants.add(change.getUser());
			}
			participants.remove(userManager.getSystem());
		}
		return participants;
	}
}
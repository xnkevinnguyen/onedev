package io.onedev.server.model;


import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import com.fasterxml.jackson.annotation.JsonView;
import io.onedev.server.util.jackson.DefaultView;
import java.util.List;
import org.eclipse.jgit.revwalk.RevCommit;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import io.onedev.server.infomanager.CommitInfoManager;
import io.onedev.server.OneDev;
import org.eclipse.jgit.lib.ObjectId;
import edu.emory.mathcs.backport.java.util.Collections;
import java.util.Comparator;
import io.onedev.server.util.facade.IssueFacade;
import io.onedev.server.util.ProjectScopedNumber;
import io.onedev.server.infomanager.PullRequestInfoManager;
import java.util.Collection;
import java.util.HashSet;
import io.onedev.server.entitymanager.PullRequestManager;
import java.io.Serializable;

public class IssueProduct2 implements Serializable {
	private String title;
	private Project project;
	private long number;
	private String noSpaceTitle;
	private transient List<RevCommit> commits;
	private transient List<PullRequest> pullRequests;

	public String getTitle() {
		return title;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public long getNumber() {
		return number;
	}

	public void setNumber(long number) {
		this.number = number;
	}

	public void setTitle(String title) {
		this.title = title;
		noSpaceTitle = StringUtils.deleteWhitespace(title);
	}

	public String getNumberAndTitle() {
		return "#" + number + " - " + title;
	}

	public List<RevCommit> getCommits() {
		if (commits == null) {
			commits = new ArrayList<>();
			CommitInfoManager commitInfoManager = OneDev.getInstance(CommitInfoManager.class);
			for (ObjectId commitId : commitInfoManager.getFixCommits(project, number)) {
				RevCommit commit = project.getRevCommit(commitId, false);
				if (commit != null)
					commits.add(commit);
			}
			Collections.sort(commits, new Comparator<RevCommit>() {
				@Override
				public int compare(RevCommit o1, RevCommit o2) {
					return o2.getCommitTime() - o1.getCommitTime();
				}
			});
		}
		return commits;
	}

	public IssueFacade getFacade(Issue issue) {
		return new IssueFacade(issue.getId(), project.getId(), number);
	}

	public ProjectScopedNumber getFQN() {
		return new ProjectScopedNumber(project, number);
	}

	public List<PullRequest> getPullRequests() {
		if (pullRequests == null) {
			pullRequests = new ArrayList<>();
			PullRequestInfoManager infoManager = OneDev.getInstance(PullRequestInfoManager.class);
			Collection<Long> pullRequestIds = new HashSet<>();
			for (ObjectId commit : getCommits())
				pullRequestIds.addAll(infoManager.getPullRequestIds(project, commit));
			for (Long requestId : pullRequestIds) {
				PullRequest request = OneDev.getInstance(PullRequestManager.class).get(requestId);
				if (request != null && !pullRequests.contains(request))
					pullRequests.add(request);
			}
			Collections.sort(pullRequests, new Comparator<PullRequest>() {
				@Override
				public int compare(PullRequest o1, PullRequest o2) {
					return o2.getId().compareTo(o1.getId());
				}
			});
		}
		return pullRequests;
	}
}
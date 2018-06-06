package io.onedev.server.model.support;

import java.io.Serializable;

import org.hibernate.validator.constraints.NotEmpty;

import io.onedev.server.util.reviewrequirement.ReviewRequirement;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.PathPattern;
import io.onedev.server.web.editable.annotation.ReviewRequirementSpec;

@Editable
public class FileProtection implements Serializable {

	private static final long serialVersionUID = 1L;

	private String path;
	
	private String reviewRequirementSpec;
	
	private transient ReviewRequirement reviewRequirement;
	
	@Editable(order=100, description="Specify path to be protected. Wildcard can be used in the path "
			+ "to match multiple files")
	@PathPattern
	@NotEmpty
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Editable(order=200, name="Reviewers", description="Optionally specify required reviewers if specified path is "
			+ "changed. Note that the user submitting the change is considered to reviewed the change automatically")
	@ReviewRequirementSpec
	@NotEmpty
	public String getReviewRequirementSpec() {
		return reviewRequirementSpec;
	}

	public void setReviewRequirementSpec(String reviewRequirementSpec) {
		this.reviewRequirementSpec = reviewRequirementSpec;
	}
	
	public ReviewRequirement getReviewRequirement() {
		if (reviewRequirement == null)
			reviewRequirement = new ReviewRequirement(reviewRequirementSpec);
		return reviewRequirement;
	}
	
}
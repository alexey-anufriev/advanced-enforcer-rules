package com.github.alexeyanufriev.advancedenforcerrules;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.utils.ArtifactMatcher;
import org.apache.maven.plugins.enforcer.utils.DependencyVersionMap;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AdvancedDependencyConvergence implements EnforcerRule {

    @Setter
    private boolean fail = true;

    @Setter
    private List<String> excludes = new ArrayList<>(0);

    @Setter
    private boolean uniqueVersions = false;

    @Override
    public void execute(@Nonnull EnforcerRuleHelper helper) throws EnforcerRuleException {
        Log logger = helper.getLog();

        DependencyVersionMap visitor = new DependencyVersionMap(logger);
        visitor.setUniqueVersions(this.uniqueVersions);

        DependencyNode dependenciesGraph = getDependenciesGraph(helper);
        dependenciesGraph.accept(visitor);

        List<List<DependencyNode>> conflictedVersions = visitor.getConflictedVersionNumbers();
        if (!conflictedVersions.isEmpty()) {
            if (!this.fail) {
                logger.info(getClass().getSimpleName() + " rule is running in reporting-only mode");
            }

            getConvergenceErrorMessages(conflictedVersions).forEach(logger::warn);

            if (this.fail) {
                throw new EnforcerRuleException("Failed while enforcing duplicated versions. "
                        + "See above detailed error message.");
            }
        }
    }

    private DependencyNode getDependenciesGraph(EnforcerRuleHelper helper) throws EnforcerRuleException {
        try {
            MavenProject project = (MavenProject) helper.evaluate("${project}");
            MavenSession session = (MavenSession) helper.evaluate("${session}");

            DependencyCollectorBuilder dependencyCollectorBuilder = helper.getComponent(DependencyCollectorBuilder.class);
            ArtifactRepository repository = (ArtifactRepository) helper.evaluate("${localRepository}");

            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
            buildingRequest.setProject(project);
            buildingRequest.setLocalRepository(repository);

            ArtifactFilter filter = getArtifactFilter();

            return dependencyCollectorBuilder.collectDependencyGraph(buildingRequest, filter);
        }
        catch (ExpressionEvaluationException | ComponentLookupException e) {
            throw new EnforcerRuleException("Unable to lookup a component " + e.getMessage(), e);
        }
        catch (DependencyCollectorBuilderException e) {
            throw new EnforcerRuleException("Could not build dependency tree " + e.getMessage(), e);
        }
    }

    private ArtifactFilter getArtifactFilter() {
        return artifact -> {
            try {
                return ("compile".equalsIgnoreCase(artifact.getScope()) || "runtime".equalsIgnoreCase(artifact.getScope()))
                        && !new ArtifactMatcher(this.excludes, Collections.emptyList()).match(artifact)
                        && !artifact.isOptional();
            }
            catch (InvalidVersionSpecificationException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        };
    }

    private List<String> getConvergenceErrorMessages(List<List<DependencyNode>> conflictedVersions) {
        return conflictedVersions.stream()
                .map(this::buildConvergenceErrorMessage)
                .collect(Collectors.toList());
    }

    private String buildConvergenceErrorMessage(List<DependencyNode> conflictedVersion) {
        StringBuilder builder = new StringBuilder();

        builder.append("Dependency convergence error for '")
                .append(conflictedVersion.get(0).getArtifact().toString())
                .append("' paths to dependency are:");

        conflictedVersion.stream()
                .map(this::buildDependencyTree)
                .collect(Collectors.toList())
                .forEach(tree -> builder.append(System.lineSeparator()).append(tree).append(System.lineSeparator()));

        return builder.toString();
    }

    private String buildDependencyTree(DependencyNode node) {
        List<String> tree = new ArrayList<>();

        DependencyNode currentNode = node;
        while (currentNode != null) {
            tree.add(currentNode.getArtifact().toString());
            currentNode = currentNode.getParent();
        }

        Collections.reverse(tree);

        return IntStream.range(0, tree.size())
                .mapToObj(index -> StringUtils.repeat("  ", index) + "└─ " + tree.get(index))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public String getCacheId() {
        return "";
    }

    @Override
    public boolean isCacheable() {
        return false;
    }

    @Override
    public boolean isResultValid(@Nonnull EnforcerRule rule) {
        return false;
    }

}

/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.regex.Pattern;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Resolves the reference given by a ref spec to the {@link ObjectId} it finally points to,
 * dereferencing symbolic refs as necessary.
 */
public class RevParse extends AbstractGeoGitOp<Optional<ObjectId>> {

    private static final char PARENT_DELIMITER = '^';

    private static final char ANCESTOR_DELIMITER = '~';

    private static final String PATH_SEPARATOR = ":";

    private String refSpec;

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-f]+$");

    private StagingDatabase indexDb;

    /**
     * Constructs a new {@code RevParse} operation with the specified {@link StagingDatabase staging
     * database}.
     * 
     * @param indexDb the staging database to use
     */
    @Inject
    public RevParse(StagingDatabase indexDb) {
        this.indexDb = indexDb;
    }

    /**
     * @param refSpec the ref spec to resolve
     * @return {@code this}
     */
    public RevParse setRefSpec(final String refSpec) {
        this.refSpec = refSpec;
        return this;
    }

    /**
     * Parses a geogit revision string and return an object id.
     * <p>
     * Combinations of these operators are supported:
     * <ul>
     * <li><b>HEAD</b>, <b>MERGE_HEAD</b>, <b>FETCH_HEAD</b></li>
     * <li><b>SHA-1</b>: a complete or abbreviated SHA-1</li>
     * <li><b>refs/...</b>: a complete reference name</li>
     * <li><b>short-name</b>: a short reference name under {@code refs/heads}, {@code refs/tags}, or
     * {@code refs/remotes} namespace</li>
     * <li><b>tag-NN-gABBREV</b>: output from describe, parsed by treating {@code ABBREV} as an
     * abbreviated SHA-1.</li>
     * <li><i>id</i><b>^</b>: first parent of commit <i>id</i>, this is the same as {@code id^1}</li>
     * <li><i>id</i><b>^0</b>: ensure <i>id</i> is a commit</li>
     * <li><i>id</i><b>^n</b>: n-th parent of commit <i>id</i></li>
     * <li><i>id</i><b>~n</b>: n-th historical ancestor of <i>id</i>, by first parent. {@code id~3}
     * is equivalent to {@code id^1^1^1} or {@code id^^^}.</li>
     * <li><i>id</i><b>:path</b>: Lookup path under tree named by <i>id</i></li>
     * <li><i>id</i><b>^{commit}</b>: ensure <i>id</i> is a commit</li>
     * <li><i>id</i><b>^{tree}</b>: ensure <i>id</i> is a tree</li>
     * <li><i>id</i><b>^{tag}</b>: ensure <i>id</i> is a tag</li>
     * <li><i>id</i><b>^{blob}</b>: ensure <i>id</i> is a blob</li>
     * </ul>
     * 
     * <p>
     * The following operators are specified by Git conventions, but are not supported by this
     * method:
     * <ul>
     * <li><b>ref@{n}</b>: n-th version of ref as given by its reflog</li>
     * <li><b>ref@{time}</b>: value of ref at the designated time</li>
     * </ul>
     * 
     * @throws IllegalArgumentException if the format of {@link #setRefSpec(String) refSpec} can't
     *         be understood, it resolves to multiple objects (e.g. a partial object id that matches
     *         multiple objects), or a precondition refSpec form was given (e.g.
     *         <code> id^{commit}</code>) and the object {@code id} resolves to is not of the
     *         expected type.
     * @return the resolved object id, may be {@link Optional#absent() absent}
     */
    @Override
    public Optional<ObjectId> call() {
        checkState(this.refSpec != null, "refSpec was not given");

        return revParse(this.refSpec);
    }

    private Optional<ObjectId> revParse(String refSpec) {

        String path = null;
        if (refSpec.contains(PATH_SEPARATOR)) {
            String[] tokens = refSpec.split(PATH_SEPARATOR);
            refSpec = tokens[0];
            path = tokens[1];
        }

        final String prefix;
        int parentN = -1;// -1 = not given, 0 = ensure id is a commit, >0 nth. parent (1 = first
                         // parent, 2 = second parent, > 2 would resolve to absent as merge commits
                         // have two parents)

        int ancestorN = -1;// -1 = not given, 0 = same commit, > 0 = nth. historical ancestor, by
                           // first parent

        RevObject.TYPE type = null;

        final StringBuilder remaining = new StringBuilder();
        if (refSpec.indexOf(PARENT_DELIMITER) > 0) {
            prefix = parsePrefix(refSpec, PARENT_DELIMITER);
            String suffix = parseSuffix(refSpec, PARENT_DELIMITER);
            if (suffix.indexOf('{') == 0) {
                type = parseType(suffix);
            } else {
                parentN = parseNumber(suffix, 1, remaining);
            }
        } else if (refSpec.indexOf(ANCESTOR_DELIMITER) > 0) {
            prefix = parsePrefix(refSpec, ANCESTOR_DELIMITER);
            String suffix = parseSuffix(refSpec, ANCESTOR_DELIMITER);
            ancestorN = parseNumber(suffix, 1, remaining);
        } else {
            prefix = refSpec;
        }

        Optional<ObjectId> resolved = resolveObject(prefix);
        if (!resolved.isPresent()) {
            return resolved;
        }

        if (parentN > -1) {
            resolved = resolveParent(resolved.get(), parentN);
        } else if (ancestorN > -1) {
            resolved = resolveAncestor(resolved.get(), ancestorN);
        } else if (type != null) {
            resolved = verifyId(resolved.get(), type);
        }
        if (resolved.isPresent() && remaining.length() > 0) {
            String newRefSpec = resolved.get().toString() + remaining.toString();
            resolved = revParse(newRefSpec);
        }

        if (!resolved.isPresent()) {
            return resolved;
        }

        if (path != null) {
            NodeRef.checkValidPath(path);
            Optional<ObjectId> treeId = command(ResolveTreeish.class).setTreeish(resolved.get())
                    .call();
            Optional<RevTree> revTree = command(RevObjectParse.class).setObjectId(treeId.get())
                    .call(RevTree.class);
            Optional<NodeRef> ref = command(FindTreeChild.class).setParent(revTree.get())
                    .setChildPath(path).call();

            if (!ref.isPresent()) {
                return Optional.absent();
            }
            resolved = Optional.of(ref.get().objectId());
        }
        return resolved;
    }

    private Optional<ObjectId> resolveParent(final ObjectId objectId, final int parentN) {
        checkNotNull(objectId);
        checkArgument(parentN > -1);
        if (objectId.isNull()) {
            return Optional.absent();
        }
        if (parentN == 0) {
            // 0 == check id is a commit
            Optional<RevObject> object = command(RevObjectParse.class).setObjectId(objectId).call();
            checkArgument(object.isPresent() && object.get() instanceof RevCommit,
                    "%s is not a commit: %s", objectId, (object.isPresent() ? object.get()
                            .getType() : "null"));
            return Optional.of(objectId);
        }

        RevCommit commit = resolveCommit(objectId);
        if (parentN > commit.getParentIds().size()) {
            return Optional.absent();
        }

        return commit.parentN(parentN - 1);
    }

    /**
     * @param objectId
     * @return
     */
    private RevCommit resolveCommit(ObjectId objectId) {

        final Optional<RevObject> object = command(RevObjectParse.class).setObjectId(objectId)
                .call();
        checkArgument(object.isPresent(), "No object named %s could be found", objectId);
        final RevObject revObject = object.get();
        RevCommit commit;
        switch (revObject.getType()) {
        case COMMIT:
            commit = (RevCommit) revObject;
            break;
        case TAG:
            ObjectId commitId = ((RevTag) revObject).getCommitId();
            commit = command(RevObjectParse.class).setObjectId(commitId).call(RevCommit.class)
                    .get();
            break;
        default:
            throw new IllegalArgumentException(String.format(
                    "%s did not resolve to a commit or tag: %s", objectId, revObject.getType()));
        }
        return commit;
    }

    private Optional<ObjectId> resolveAncestor(ObjectId objectId, int ancestorN) {
        RevCommit commit = resolveCommit(objectId);
        if (ancestorN == 0) {
            return Optional.of(commit.getId());
        }
        Optional<ObjectId> firstParent = commit.parentN(0);
        if (!firstParent.isPresent()) {
            return Optional.absent();
        }
        return resolveAncestor(firstParent.get(), ancestorN - 1);
    }

    private Optional<ObjectId> verifyId(ObjectId objectId, RevObject.TYPE type) {
        final Optional<RevObject> object = command(RevObjectParse.class).setObjectId(objectId)
                .call();

        checkArgument(object.isPresent(), "No object named %s could be found", objectId);
        final RevObject revObject = object.get();

        if (type.equals(revObject.getType())) {
            return Optional.of(revObject.getId());
        } else {
            throw new IllegalArgumentException(String.format("%s did not resolve to %s: %s",
                    objectId, type, revObject.getType()));
        }
    }

    private int parseNumber(final String suffix, final int defaultValue,
            StringBuilder remainingTarget) {
        if (suffix.isEmpty() || !Character.isDigit(suffix.charAt(0))) {
            remainingTarget.append(suffix);
            return defaultValue;
        }

        int i = 0;
        StringBuilder sb = new StringBuilder();
        while (i < suffix.length() && Character.isDigit(suffix.charAt(i))) {
            sb.append(suffix.charAt(i));
            i++;
        }
        remainingTarget.append(suffix.substring(i));
        return Integer.parseInt(sb.toString());
    }

    private String parseSuffix(final String spec, final char delim) {
        checkArgument(spec.indexOf(delim) > -1);
        String suffix = spec.substring(spec.indexOf(delim) + 1);
        return suffix;
    }

    private String parsePrefix(String spec, char delim) {
        checkArgument(spec.indexOf(delim) > -1);
        return spec.substring(0, spec.indexOf(delim));
    }

    private RevObject.TYPE parseType(String spec) {
        String type = spec.substring(1, spec.length() - 1);

        if (type.equals("commit")) {
            return RevObject.TYPE.COMMIT;
        } else if (type.equals("tree")) {
            return RevObject.TYPE.TREE;
        } else if (type.equals("tag")) {
            return RevObject.TYPE.TAG;
        } else if (type.equals("feature")) {
            return RevObject.TYPE.FEATURE;
        }
        throw new IllegalArgumentException(String.format("%s did not resolve to a type", type));
    }

    /**
     * @param objectName a ref name or object id
     */
    private Optional<ObjectId> resolveObject(final String refSpec) {
        ObjectId resolvedTo = null;

        // is it a ref?
        Optional<Ref> ref = command(RefParse.class).setName(refSpec).call();
        if (ref.isPresent()) {
            resolvedTo = ref.get().getObjectId();
        } else {
            // does it look like an object id hash?
            boolean hexPatternMatches = HEX_PATTERN.matcher(refSpec).matches();
            if (hexPatternMatches) {
                try {
                    if (ObjectId.valueOf(refSpec).isNull()) {
                        return Optional.of(ObjectId.NULL);
                    }
                } catch (IllegalArgumentException ignore) {
                    // its a partial id
                }
                List<ObjectId> hashMatches = indexDb.lookUp(refSpec);
                if (hashMatches.size() > 1) {
                    throw new IllegalArgumentException(String.format(
                            "Ref spec (%s) matches more than one object id: %s", refSpec,
                            hashMatches.toString()));
                }
                if (hashMatches.size() == 1) {
                    resolvedTo = hashMatches.get(0);
                }
            }
        }
        return Optional.fromNullable(resolvedTo);
    }
}

import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RefUpdate;

import java.util.Date;
import java.util.TimeZone;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.text.MessageFormat;



public class JGitUtil {

    public static void delete(final Git git, final String branchName, final String path,
            final String name, final String email, final String message, final TimeZone timeZone, final Date when) {
        commit(git, branchName, path, null, name, email, message, timeZone, when);
    }

    public static void commit(final Git git, final String branchName, final String path, final File file,
            final String name, final String email, final String message, final TimeZone timeZone, final Date when) {

        //TODO implemente fixPath
        //I sent a message to porcelli on linkedin to understand how fixPath works
        final String gitPath = "path";//fixPath(path);

        final PersonIdent author = buildPersonIdent(git, name, email, timeZone, when);

        try {
            final ObjectInserter odi = git.getRepository().newObjectInserter();
            try {
                // Create the in-memory index of the new/updated issue.
                final ObjectId headId = git.getRepository().resolve(branchName + "^{commit}");
                final DirCache index = createTemporaryIndex(git, headId, gitPath, file);
                final ObjectId indexTreeId = index.writeTree(odi);

                // Create a commit object
                final CommitBuilder commit = new CommitBuilder();
                commit.setAuthor(author);
                commit.setCommitter(author);
                commit.setEncoding(Constants.CHARACTER_ENCODING);
                commit.setMessage(message);
                //headId can be null if the repository has no commit yet
                if (headId != null) {
                    commit.setParentId(headId);
                }
                commit.setTreeId(indexTreeId);

                // Insert the commit into the repository
                final ObjectId commitId = odi.insert(commit);
                odi.flush();

                final RevWalk revWalk = new RevWalk(git.getRepository());
                try {
                    final RevCommit revCommit = revWalk.parseCommit(commitId);
                    final RefUpdate ru = git.getRepository().updateRef("refs/heads/" + branchName);
                    if (headId == null) {
                        ru.setExpectedOldObjectId(ObjectId.zeroId());
                    } else {
                        ru.setExpectedOldObjectId(headId);
                    }
                    ru.setNewObjectId(commitId);
                    ru.setRefLogMessage("commit: " + revCommit.getShortMessage(), false);
                    final RefUpdate.Result rc = ru.forceUpdate();
                    switch (rc) {
                        case NEW:
                        case FORCED:
                        case FAST_FORWARD:
                            break;
                        case REJECTED:
                        case LOCK_FAILURE:
                            throw new ConcurrentRefUpdateException(JGitText.get().couldNotLockHEAD, ru.getRef(), rc);
                        default:
                            throw new JGitInternalException(MessageFormat.format(JGitText.get().updatingRefFailed, Constants.HEAD, commitId.toString(), rc));
                    }

                } finally {
              //TODO Doesn't worked.
              //I think the method has been discontinued
              // symbol:   method release()
                //revWalk.release();
                }
            } finally {
              //TODO Doesn't worked.
              //I think the method has been discontinued
              // symbol:   method release()
              //  odi.release();
            }
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static PersonIdent buildPersonIdent(final Git git, final String name, final String email,
            final TimeZone timeZone, final Date when) {
        final TimeZone tz = timeZone == null ? TimeZone.getDefault() : timeZone;

        if (name != null) {
            if (when != null) {
                return new PersonIdent(name, email, when, tz);
            } else {
                return new PersonIdent(name, email);
            }
        }
        return new PersonIdent(git.getRepository());
    }

    /**
     * Creates an in-memory index of the issue change.
     */
    private static DirCache createTemporaryIndex(final Git git, final ObjectId headId, final String path, final File file) {

        final DirCache inCoreIndex = DirCache.newInCore();
        final DirCacheBuilder dcBuilder = inCoreIndex.builder();
        final ObjectInserter inserter = git.getRepository().newObjectInserter();

        try {
            if (file != null) {
                final DirCacheEntry dcEntry = new DirCacheEntry(path);
                dcEntry.setLength(file.length());
                dcEntry.setLastModified(file.lastModified());
                dcEntry.setFileMode(FileMode.REGULAR_FILE);

                final InputStream inputStream = new FileInputStream(file);
                try {
                    dcEntry.setObjectId(inserter.insert(Constants.OBJ_BLOB, file.length(), inputStream));
                } finally {
                    inputStream.close();
                }

                dcBuilder.add(dcEntry);
            }

            if (headId != null) {
                final TreeWalk treeWalk = new TreeWalk(git.getRepository());
                final int hIdx = treeWalk.addTree(new RevWalk(git.getRepository()).parseTree(headId));
                treeWalk.setRecursive(true);

                while (treeWalk.next()) {
                    final String walkPath = treeWalk.getPathString();
                    final CanonicalTreeParser hTree = treeWalk.getTree(hIdx, CanonicalTreeParser.class);

                    if (!walkPath.equals(path)) {
                        // add entries from HEAD for all other paths
                        // create a new DirCacheEntry with data retrieved from HEAD
                        final DirCacheEntry dcEntry = new DirCacheEntry(walkPath);
                        dcEntry.setObjectId(hTree.getEntryObjectId());
                        dcEntry.setFileMode(hTree.getEntryFileMode());

                        // add to temporary in-core index
                        dcBuilder.add(dcEntry);
                    }
                }
              //TODO Doesn't worked.
              //I think the method has been discontinued
              // symbol:   method release()
              //  treeWalk.release();
            }

            dcBuilder.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
              //TODO Doesn't worked.
              //I think the method has been discontinued
              // symbol:   method release()
              //inserter.release();
        }

        if (file == null) {
            final DirCacheEditor editor = inCoreIndex.editor();
            editor.add(new DirCacheEditor.DeleteTree(path));
            editor.finish();
        }

        return inCoreIndex;
    }
}

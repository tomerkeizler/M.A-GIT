package Engine.Branches;

import java.nio.file.Paths;

public class RemoteBranch extends Branch {

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public RemoteBranch(String remoteRepositoryName, String branchName) {
        super(remoteRepositoryName + "\\" + branchName);
    }

    public RemoteBranch(String remoteRepositoryName, String branchName, String commitSha1) {
        super(remoteRepositoryName + "\\" + branchName, commitSha1);
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public String getRemoteRepositoryName() {
        return Paths.get(branchName).getParent().toString();
    }

    public String getOriginalBranchName() {
        return Paths.get(branchName).getFileName().toString();
    }


}

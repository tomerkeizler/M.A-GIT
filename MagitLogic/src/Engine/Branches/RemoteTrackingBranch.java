package Engine.Branches;

public class RemoteTrackingBranch extends Branch {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private String trackedRemoteBranch;

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public RemoteTrackingBranch(String trackedRemoteBranch, String branchName) {
        super(branchName);
        this.trackedRemoteBranch = trackedRemoteBranch;
    }

    public RemoteTrackingBranch(String trackedRemoteBranch, String branchName, String commitSha1) {
        super(branchName, commitSha1);
        this.trackedRemoteBranch = trackedRemoteBranch;
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public String getTrackedRemoteBranch() {
        return trackedRemoteBranch;
    }

    public void setTrackedRemoteBranch(String trackedRemoteBranch) {
        this.trackedRemoteBranch = trackedRemoteBranch;
    }

}

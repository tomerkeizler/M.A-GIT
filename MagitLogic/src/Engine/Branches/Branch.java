package Engine.Branches;

public class Branch {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    protected String branchName;
    protected String commitSha1;

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public Branch(String branchName) {
        this.branchName = branchName;
        this.commitSha1 = null;
    }

    public Branch(String branchName, String commitSha1) {
        this.branchName = branchName;
        this.commitSha1 = commitSha1;
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public String getBranchName() {
        return branchName;
    }

    public String getCommitSha1() {
        return commitSha1;
    }

    public void setCommitSha1(String currentCommitSha1) {
        this.commitSha1 = currentCommitSha1;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.branchName.equals(((Branch) obj).branchName);
    }

    @Override
    public String toString() {
        return String.format("%s,%s", branchName, commitSha1);
    }

}

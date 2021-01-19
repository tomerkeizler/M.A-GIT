package Engine.GitObjects;

import puk.team.course.magit.ancestor.finder.CommitRepresentative;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Commit extends GitObject implements CommitRepresentative, Comparable {

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private String rootFolderSha1;
    private String previousCommitSha1;
    private String secondPreviousCommitSha1;
    private String commitContent;

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public Commit(String lastModifier, String lastModifyDate, String rootFolderSha1, String previousCommitSha1, String secondPreviousCommitSha1, String commitContent) {
        super(lastModifier, lastModifyDate);
        this.rootFolderSha1 = rootFolderSha1;
        this.previousCommitSha1 = previousCommitSha1;
        this.secondPreviousCommitSha1 = secondPreviousCommitSha1;
        this.commitContent = commitContent;
        setSha1();
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public String getRootFolderSha1() {
        return rootFolderSha1;
    }

    public String getFirstPrecedingSha1() {
        return previousCommitSha1 == null ? "" : previousCommitSha1;
    }

    public String getSecondPrecedingSha1() {
        return secondPreviousCommitSha1 == null ? "" : secondPreviousCommitSha1;
    }

    public String getCommitContent() {
        return commitContent;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public boolean isOrphanCommit() {
        return getFirstPrecedingSha1().isEmpty() && getSecondPrecedingSha1().isEmpty();
    }


    public String getObjectDetails() {
        return String.format("%s,%s,%s,%s", sha1, commitContent, lastModifier, lastModifyDate);
    }

    public String getObjectContent() {
        return String.format("%s\n%s\n%s\n%s\n%s\n%s", rootFolderSha1, previousCommitSha1, secondPreviousCommitSha1, commitContent, lastModifyDate, lastModifier);
    }

    public Date convertStringToDate(String date) throws ParseException {
        return dateFormat.parse(date);
    }

    @Override
    public boolean equals(Object obj) {
        return getSha1().equals(((Commit) obj).getSha1());
    }

    @Override
    public int compareTo(Object obj) {
        Date thisDate = null;
        Date otherDate = null;
        try {
            thisDate = convertStringToDate(lastModifyDate);
            otherDate = convertStringToDate(((Commit) obj).getLastModifyDate());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return otherDate.compareTo(thisDate);
    }

    @Override
    public String toString() {
        return String.format("Commit \"%s\" (by %s)", getCommitContent(), getLastModifier());
    }

    public String getCompleteInformation() {
        return String.format("Commit SHA-1: %s\n\n", getSha1()) +
                String.format("Commit message: %s\n\n", getCommitContent()) +
                String.format("Commit modifier: %s\n\n", getLastModifier()) +
                String.format("Commit creation time: %s", getLastModifyDate());
    }

}

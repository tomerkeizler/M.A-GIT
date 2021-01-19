package Magit;

import Engine.Repository;


public abstract class InternalController {

    protected MagitController magitController;

    //------------------------------------- //
    //-------------- Methods -------------- //
    //------------------------------------- //

    public Repository getRepository() {
        return magitController.getMagit().getRepository();
    }

    public void setMainController(MagitController magitController) {
        this.magitController = magitController;
    }

}

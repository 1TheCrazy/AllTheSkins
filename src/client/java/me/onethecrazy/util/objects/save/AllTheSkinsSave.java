package me.onethecrazy.util.objects.save;

public class AllTheSkinsSave {
    public ClientSkin selectedSkin;
    public boolean isEnabled;

    public AllTheSkinsSave(){
        this.selectedSkin = new ClientSkin();
        this.isEnabled = true;
    }
}

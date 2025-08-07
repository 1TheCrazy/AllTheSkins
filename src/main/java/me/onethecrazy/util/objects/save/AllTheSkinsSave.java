package me.onethecrazy.util.objects.save;

public class AllTheSkinsSave {
    public Skin selectedSkin;
    public boolean isEnabled;

    public AllTheSkinsSave(){
        this.selectedSkin = new Skin();
        this.isEnabled = true;
    }
}

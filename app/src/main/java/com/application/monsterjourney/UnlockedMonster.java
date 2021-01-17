package com.application.monsterjourney;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "UNLOCKEDMONSTER")
public class UnlockedMonster {
    /**
     * class info for if we have unlocked the specified monster yet
     */
    @PrimaryKey(autoGenerate=true)
    public int uid;

    @ColumnInfo(name = "monsterarrayid")
    int monsterarrayid;

    //where stage 0 is egg, stage 1 is baby, 2 is child, and 3 is adult
    @ColumnInfo(name = "stage")
    int stage;

    @ColumnInfo(name = "unlocked")
    boolean unlocked;

    @ColumnInfo(name = "discovered")
    boolean discovered;

    public UnlockedMonster(int monsterarrayid,int stage, boolean unlocked){
        this.monsterarrayid = monsterarrayid;
        this.unlocked = unlocked;
        discovered = unlocked;
        this.stage = stage;
    }

    public static UnlockedMonster[] populateData(){
        return new UnlockedMonster[] {
                new UnlockedMonster(R.array.enigma_egg,0, true),
                new UnlockedMonster(R.array.dino_egg, 0,true),
                new UnlockedMonster(R.array.earth_egg, 0, false),
                new UnlockedMonster(R.array.fire_egg,0, false),
                new UnlockedMonster(R.array.machine_egg, 0,false),
                new UnlockedMonster(R.array.aqua_egg, 0, false),
                new UnlockedMonster(R.array.enigma_baby1,1, false),
                new UnlockedMonster(R.array.enigma_baby2,1, false),
                new UnlockedMonster(R.array.aqua_baby1,1, false),
                new UnlockedMonster(R.array.aqua_baby2,1, false),
                new UnlockedMonster(R.array.dino_baby1,1, false),
                new UnlockedMonster(R.array.dino_baby2, 1,false),
                new UnlockedMonster(R.array.earth_baby1,1, false),
                new UnlockedMonster(R.array.earth_baby2, 1,false),
                new UnlockedMonster(R.array.enigma_child_tanuki,2, false),
                new UnlockedMonster(R.array.enigma_child_mushroom,2, false),
                new UnlockedMonster(R.array.enigma_child_snake,2, false),
                new UnlockedMonster(R.array.enigma_child_floatingskull,2, false),
                new UnlockedMonster(R.array.enigma_child_ghost,2, false),
                new UnlockedMonster(R.array.aqua_child_bubblething,2, false),
                new UnlockedMonster(R.array.aqua_child_clam,2, false),
                new UnlockedMonster(R.array.aqua_child_eelthing,2, false),
                new UnlockedMonster(R.array.aqua_child_pufferfish,2, false),
                new UnlockedMonster(R.array.aqua_child_watertoad,2, false),
                new UnlockedMonster(R.array.dino_child_crocodile,2, false),
                new UnlockedMonster(R.array.dino_child_longneck, 2,false),
                new UnlockedMonster(R.array.dino_child_armadillo, 2,false),
                new UnlockedMonster(R.array.dino_child_stegosaur, 2,false),
                new UnlockedMonster(R.array.dino_child_triceratops,2, false),
                new UnlockedMonster(R.array.earth_child_beetle,2, false),
                new UnlockedMonster(R.array.earth_child_plant, 2,false),
                new UnlockedMonster(R.array.earth_child_cabbage, 2,false),
                new UnlockedMonster(R.array.earth_child_bush, 2,false),
                new UnlockedMonster(R.array.earth_child_squirrel,2, false),
                new UnlockedMonster(R.array.enigma_adult_rainspirit, 3,false),
                new UnlockedMonster(R.array.enigma_adult_snowman, 3,false),
                new UnlockedMonster(R.array.enigma_adult_teddybear, 3,false),
                new UnlockedMonster(R.array.enigma_adult_scarecrow, 3,false),
                new UnlockedMonster(R.array.enigma_adult_sphinx, 3,false),
                new UnlockedMonster(R.array.enigma_adult_specter, 3,false),
                new UnlockedMonster(R.array.enigma_adult_electricball, 3,false),
                new UnlockedMonster(R.array.enigma_adult_vampireking, 3,false),
                new UnlockedMonster(R.array.enigma_adult_frankenstein, 3,false),
                new UnlockedMonster(R.array.dino_adult_plesiosaur, 3,false),
                new UnlockedMonster(R.array.dino_adult_ankylosaur, 3,false),
                new UnlockedMonster(R.array.dino_adult_sloth, 3,false),
                new UnlockedMonster(R.array.dino_adult_bigheaddino, 3,false),
                new UnlockedMonster(R.array.dino_adult_brachiosaur, 3,false),
                new UnlockedMonster(R.array.dino_adult_sabretooth, 3,false),
                new UnlockedMonster(R.array.dino_adult_spinosaur, 3,false),
                new UnlockedMonster(R.array.dino_adult_horneddino, 3,false),
                new UnlockedMonster(R.array.earth_adult_beetle, 3,false),
                new UnlockedMonster(R.array.earth_adult_butterfly, 3,false),
                new UnlockedMonster(R.array.earth_adult_stump, 3,false),
                new UnlockedMonster(R.array.earth_adult_watermelon, 3,false),
                new UnlockedMonster(R.array.earth_adult_moose, 3,false),
                new UnlockedMonster(R.array.earth_adult_monkey, 3,false),
                new UnlockedMonster(R.array.earth_adult_blob, 3,false),
                new UnlockedMonster(R.array.earth_adult_eggimitate, 3,false),
                new UnlockedMonster(R.array.earth_adult_leafcat, 3,false),
                new UnlockedMonster(R.array.aqua_adult_footballfish, 3,false),
                new UnlockedMonster(R.array.aqua_adult_jellyfish, 3,false),
                new UnlockedMonster(R.array.aqua_adult_octopus, 3,false),
                new UnlockedMonster(R.array.aqua_adult_scalyfootsnail, 3,false),
                new UnlockedMonster(R.array.aqua_adult_stingray, 3,false),
                new UnlockedMonster(R.array.aqua_adult_walrus, 3,false),
                new UnlockedMonster(R.array.aqua_adult_waterdragon, 3,false),
                new UnlockedMonster(R.array.aqua_adult_whaleseal, 3,false),
                new UnlockedMonster(R.array.machine_baby1,1, false),
                new UnlockedMonster(R.array.machine_baby2, 1,false),
                new UnlockedMonster(R.array.machine_child_oven,2, false),
                new UnlockedMonster(R.array.machine_child_plug, 2,false),
                new UnlockedMonster(R.array.machine_child_tank, 2,false),
                new UnlockedMonster(R.array.machine_child_toy, 2,false),
                new UnlockedMonster(R.array.machine_child_ufo,2, false),
                new UnlockedMonster(R.array.machine_adult_barrelchest, 3,false),
                new UnlockedMonster(R.array.machine_adult_blimp, 3,false),
                new UnlockedMonster(R.array.machine_adult_driller, 3,false),
                new UnlockedMonster(R.array.machine_adult_excavator, 3,false),
                new UnlockedMonster(R.array.machine_adult_longarm, 3,false),
                new UnlockedMonster(R.array.machine_adult_robocrab, 3,false),
                new UnlockedMonster(R.array.machine_adult_robotdog, 3,false),
                new UnlockedMonster(R.array.machine_adult_robotowl, 3,false),
                new UnlockedMonster(R.array.machine_adult_spidermech, 3,false),
                new UnlockedMonster(R.array.fire_baby1,1, false),
                new UnlockedMonster(R.array.fire_baby2, 1,false),
                new UnlockedMonster(R.array.fire_child_chameleon,2, false),
                new UnlockedMonster(R.array.fire_child_fireball, 2,false),
                new UnlockedMonster(R.array.fire_child_seaturtle, 2,false),
                new UnlockedMonster(R.array.fire_child_toad, 2,false),
                new UnlockedMonster(R.array.fire_child_wyvern,2, false),
                new UnlockedMonster(R.array.fire_adult_baby, 3,false),
                new UnlockedMonster(R.array.fire_adult_elemental, 3,false),
                new UnlockedMonster(R.array.fire_adult_factorygorilla, 3,false),
                new UnlockedMonster(R.array.fire_adult_fireball, 3,false),
                new UnlockedMonster(R.array.fire_adult_firelordgoat, 3,false),
                new UnlockedMonster(R.array.fire_adult_fox, 3,false),
                new UnlockedMonster(R.array.fire_adult_serpent, 3,false),
                new UnlockedMonster(R.array.fire_adult_tikimask, 3,false),
                new UnlockedMonster(R.array.fire_adult_spirit, 3,false),
                new UnlockedMonster(R.array.fire_adult_snake, 3,false),
                new UnlockedMonster(R.array.dark_egg, 0,false),
                new UnlockedMonster(R.array.dark_baby1,1, false),
                new UnlockedMonster(R.array.dark_baby2, 1,false),
                new UnlockedMonster(R.array.dark_child_bigmouth,2, false),
                new UnlockedMonster(R.array.dark_child_crystal, 2,false),
                new UnlockedMonster(R.array.dark_child_egghide, 2,false),
                new UnlockedMonster(R.array.dark_child_flyingeye, 2,false),
                new UnlockedMonster(R.array.dark_child_salamander,2, false),
                new UnlockedMonster(R.array.dark_adult_anglerfish, 3,false),
                new UnlockedMonster(R.array.dark_adult_bear, 3,false),
                new UnlockedMonster(R.array.dark_adult_cthulu, 3,false),
                new UnlockedMonster(R.array.dark_adult_eyeball, 3,false),
                new UnlockedMonster(R.array.dark_adult_gorilla, 3,false),
                new UnlockedMonster(R.array.dark_adult_greedfox, 3,false),
                new UnlockedMonster(R.array.dark_adult_scorpion, 3,false),
                new UnlockedMonster(R.array.dark_adult_sloth, 3,false),
                new UnlockedMonster(R.array.dark_adult_wolf, 3,false),
                new UnlockedMonster(R.array.light_egg, 0,false),
                new UnlockedMonster(R.array.light_baby1,1, false),
                new UnlockedMonster(R.array.light_baby2, 1,false),
                new UnlockedMonster(R.array.light_child_amulet,2, false),
                new UnlockedMonster(R.array.light_child_angel, 2,false),
                new UnlockedMonster(R.array.light_child_angeldoctor, 2,false),
                new UnlockedMonster(R.array.light_child_crystal, 2,false),
                new UnlockedMonster(R.array.light_child_turtle,2, false),
                new UnlockedMonster(R.array.light_adult_armour, 3,false),
                new UnlockedMonster(R.array.light_adult_butterfly, 3,false),
                new UnlockedMonster(R.array.light_adult_centaur, 3,false),
                new UnlockedMonster(R.array.light_adult_dragon, 3,false),
                new UnlockedMonster(R.array.light_adult_frog, 3,false),
                new UnlockedMonster(R.array.light_adult_holyring, 3,false),
                new UnlockedMonster(R.array.light_adult_kingarmor, 3,false),
                new UnlockedMonster(R.array.light_adult_snake, 3,false),
                new UnlockedMonster(R.array.light_adult_wyvern, 3,false),
                new UnlockedMonster(R.array.cosmic_egg, 0,false),
                new UnlockedMonster(R.array.cosmic_baby1,1, false),
                new UnlockedMonster(R.array.cosmic_baby2, 1,false),
                new UnlockedMonster(R.array.cosmic_child_gemini,2, false),
                new UnlockedMonster(R.array.cosmic_child_lyra, 2,false),
                new UnlockedMonster(R.array.cosmic_child_pisces, 2,false),
                new UnlockedMonster(R.array.cosmic_child_scorpio, 2,false),
                new UnlockedMonster(R.array.cosmic_child_ursaminor,2, false),
                new UnlockedMonster(R.array.cosmic_adult_aquarius, 3,false),
                new UnlockedMonster(R.array.cosmic_adult_aries, 3,false),
                new UnlockedMonster(R.array.cosmic_adult_cancer, 3,false),
                new UnlockedMonster(R.array.cosmic_adult_capricorn, 3,false),
                new UnlockedMonster(R.array.cosmic_adult_leo, 3,false),
                new UnlockedMonster(R.array.cosmic_adult_libra, 3,false),
                new UnlockedMonster(R.array.cosmic_adult_sagittarius, 3,false),
                new UnlockedMonster(R.array.cosmic_adult_tauros, 3,false),
                new UnlockedMonster(R.array.cosmic_adult_virgo, 3,false)
        };
    }

    public int getMonsterarrayid(){return monsterarrayid;}

    public void setUnlocked(boolean unlocked){this.unlocked = unlocked;}

    public boolean isUnlocked(){return unlocked;}

    public void setDiscovered(boolean discovered){this.discovered = discovered;}

    public boolean isDiscovered(){return discovered;}

    public int getStage(){return stage;}
}

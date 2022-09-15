package progThiever;

import io.netty.util.internal.ThreadLocalRandom;
import org.dreambot.api.Client;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.SkillTracker;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.listener.ChatListener;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.widgets.message.Message;

import java.awt.*;

import static org.dreambot.api.methods.MethodProvider.log;
import static org.dreambot.api.methods.tabs.Tabs.logout;

@ScriptManifest(name = "Brotato Fruit Thiever", description = "Steals fruit in Hosidius (near magic trees) until lvl 55 thieving", author = "JavaBrotato",
        version = 2.0, category = Category.THIEVING, image = "")
public final class brotatoFruitStallThiever extends AbstractScript implements ChatListener {
    State state;
    final Area fruitStallArea = new Area(1795, 3609, 1801, 3606);
    String s;
    String stateForDebugging;
    long startTime;
    int oldThievExp;
    int fruitStolen;
    //start at 25, afk every 25-50
    int actionsTillAfk = 25;
    int closedDialogues;

    @Override
    public void onMessage(Message m) {
        if (m.getMessage().contains("You steal")) {

            fruitStolen++;
            actionsTillAfk--;
        }
    }

    @Override // Infinite loop
    public int onLoop() {

        switch (getState()) {

            case LOGOUT:
                log("Target level reached -- logging out.");
                logout();
                break;

            case THIEVING:
                stateForDebugging = "THIEVING";
                GameObject stall = GameObjects.closest("Fruit stall");
                handleDialogues();
                if (stall == null) {
                    s = "Waiting for stall";
                    sleep(1000);
                }
                if (actionsTillAfk <= 1) {
                    log("here actions <1");
                    sleep(customSleepFunction());
                }
                s = "All is good, thieving away";
                stall.interact("Steal-from");
                sleep(randomNum(300,700));


                break;
            case DROP:
                stateForDebugging = "DROP";
                s = "Inventory full - dropping all fruit except strange fruit";
                Inventory.dropAll(i -> i != null && !i.getName().contains("Coins"));
                break;
            case OUTOFPOSITION:
                stateForDebugging = "OUTOFPOSITION";
                logout();
                break;

        }
        return 1000;
    }

    private enum State {
        LOGOUT, THIEVING, DROP, OUTOFPOSITION


    }

    private State getState() {

        if (Client.isLoggedIn() && fruitStallArea.contains(player())) {
            state = State.THIEVING;
        }
        if (!fruitStallArea.contains(player())) {
            state = State.OUTOFPOSITION;
        }
        if (getSkills().getRealLevel(Skill.THIEVING) >= 50) {
            state = State.LOGOUT;
        }
        if (Inventory.isFull())
            return State.DROP;
        return state;
    }

    public void onStart() {
        doActionsOnStart();
    }

    public void onExit() {
        doActionsOnExit();
    }




    @Override
    public void onPaint(Graphics g) {
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.drawString(s, 12, 320);
        g.drawString("Fruit Stolen: " + fruitStolen, 12, 300);
        g.drawString("Current state: " + stateForDebugging, 12, 280);
        g.drawString("Actions till AFK: " + actionsTillAfk, 12, 260);
        g.drawString("Run time: " + getElapsedTimeAsString(), 12, 240);
        g.drawString("Thieving Lvl: " + Skills.getBoostedLevels(Skill.THIEVING), 12, 220);
        g.drawString("Exp/Hr: " + SkillTracker.getGainedExperiencePerHour(Skill.THIEVING), 12, 200);
        g.drawString("Time Till Level: " + makeTimeString(SkillTracker.getTimeToLevel(Skill.THIEVING)), 12, 180);


    }


    // HELPER METHODS
    private void doActionsOnStart() {
        startTime = System.currentTimeMillis(); //save current number of system millis elapsed (long value)
        SkillTracker.start(Skill.THIEVING); //see https://dreambot.org/javadocs/org/dreambot/api/methods/skills/SkillTracker.html
        oldThievExp = Skills.getExperience(Skill.THIEVING);

        log("Starting Brotato Script --- inspired by Zuul Gnome Stronghold Agility Script");
    }

    private void doActionsOnExit() {
        log(String.format("Gained Thieving xp: %d", (Skills.getExperience(Skill.THIEVING) - oldThievExp)));
        log("Runtime: " + getElapsedTimeAsString());
    }


    private void handleDialogues() {
        if (Dialogues.inDialogue()) { // see https://dreambot.org/javadocs/org/dreambot/api/methods/dialogues/Dialogues.html
            for (int i = 0; i<4; i++) {
                if (Dialogues.canContinue()) { //
                    Dialogues.continueDialogue(); //
                    sleep(nextInt(500, 750)); //
                } else {
                    break; //break out of loop, if no more dialogues
                }
            }
            closedDialogues++;
        }
    }


    private String makeTimeString(long ms) {
        final int seconds = (int) (ms / 1000) % 60;
        final int minutes = (int) ((ms / (1000 * 60)) % 60);
        final int hours = (int) ((ms / (1000 * 60 * 60)) % 24);
        final int days = (int) ((ms / (1000 * 60 * 60 * 24)) % 7);
        final int weeks = (int) (ms / (1000 * 60 * 60 * 24 * 7));
        if (weeks > 0) {
            return String.format("%02dw %03dd %02dh %02dm %02ds", weeks, days, hours, minutes, seconds);
        }
        if (weeks == 0 && days > 0) {
            return String.format("%03dd %02dh %02dm %02ds", days, hours, minutes, seconds);
        }
        if (weeks == 0 && days == 0 && hours > 0) {
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
        }
        if (weeks == 0 && days == 0 && hours == 0 && minutes > 0) {
            return String.format("%02dm %02ds", minutes, seconds);
        }
        if (weeks == 0 && days == 0 && hours == 0 && minutes == 0) {
            return String.format("%02ds", seconds);
        }
        if (weeks == 0 && days == 0 && hours == 0 && minutes == 0 && seconds == 0) {
            return String.format("%04dms", ms);
        }
        return "00";
    }
    public int randomNum(int i, int k) {
        //Custom Random Number Generator
        int number = (int) (Math.random() * (k - i)) + i;
        return number;
    }

    public int customSleepFunction() {
        actionsTillAfk = randomNum(25, 50);
        int randomSleepTime = randomNum(15000, 45000);
        log("Bot sleeping for: " + randomSleepTime);
        return randomSleepTime;
    }
    private long getElapsedTime() {
        return System.currentTimeMillis() - startTime; //return elapsed millis since start of script
    }

    private String getElapsedTimeAsString() {
        return makeTimeString(getElapsedTime()); //make a formatted string from a long value
    }


    private int nextInt(int lowValIncluded, int highValExcluded) { //get a random value between a range, high end is not included
        return ThreadLocalRandom.current().nextInt();
    }

    private int nextInt(int valIncluded) { //get a random value
        return ThreadLocalRandom.current().nextInt(valIncluded);
    }

    private Player player() { //get the local player, less typing
        return getLocalPlayer();
    }

    private int playerX() { //get player x location
        return player().getX();
    }

    private int playerY() { //get player y location
        return player().getY();
    }

    private int playerZ() { //get player z location
        return player().getZ();
    }

    private boolean isMoving() { //true if player is moving
        return player().isMoving();
    }

    private boolean isAnimating() { //true if player is animating. NOT all animations in game results in a true return value
        return player().isAnimating(); //eg. walking on a agility log (arms stretched to the sides), does not return true here
    }


}


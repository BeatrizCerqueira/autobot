package autobot;

import robocode.AdvancedRobot;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
/* # Objective
 *
 * Detect an energy drop to know when a bullet was fired and trace its trajectory
 * Identify multiple bullets
 * remove draw when exceeds arena's area
 *
 */

/* # Useful information
 *
 * After firing, a robot's gun heats up to a value of: 1 + (bulletPower / 5)
 * Bullet velocity 20 - 3 * firepower.
 * Collision damage = abs(velocity) * 0.5 - 1 | max = 8*0.5 -1 = 3,
 * The default cooling rate in Robocode is 0.1 per tick.
 *
 */

public class TrackBulletsBia extends AdvancedRobot {

    // Paint/Debug properties
    final double RADAR_COVERAGE_DIST = 10; // Distance we want to scan from middle of enemy to either side
    boolean scannedBot = false;
    double enemy_energy = 100;
    double enemy_heat = 2.8;    //initial heat

    ArrayList<Bullet> bullets = new ArrayList<>();
    Point2D robotLocation;
    Point2D enemyLocation;

    public void run() {
        setAdjustRadarForGunTurn(true);

        //noinspection InfiniteLoopStatement
        do {
            robotLocation = new Point2D.Double(getX(), getY());

            if (getRadarTurnRemaining() == 0.0)
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

            execute();

        } while (true);
    }

    private void setRadarTurn(ScannedRobotEvent e) {

        // Get the enemy angle
        double enemyAngle = getHeading() + e.getBearing();

        // Relativize enemy angle
        double radarInitialTurn = Utils.normalRelativeAngleDegrees(enemyAngle - getRadarHeading());

        // Radar goes that much further in the direction it is going to turn
        double extraRadarTurn = Math.toDegrees(Math.atan(RADAR_COVERAGE_DIST / e.getDistance()));
        double radarTotalTurn = radarInitialTurn + (extraRadarTurn * Math.signum(radarInitialTurn));

        // Radar goes to the less distance direction
        double normalizedRadarTotalTurn = Utils.normalRelativeAngleDegrees(radarTotalTurn);
        double radarTurn = (Math.min(Math.abs(normalizedRadarTotalTurn), Rules.RADAR_TURN_RATE)) * Math.signum(normalizedRadarTotalTurn);

        // Set radar turn
        setTurnRadarRight(radarTurn);
    }

    private void setGunTurn(ScannedRobotEvent e) {

        // Get the enemy angle
        double enemyAngle = getHeading() + e.getBearing();

        // Relativize enemy angle
        double gunInitialTurn = Utils.normalRelativeAngleDegrees(enemyAngle - getGunHeading());

        // Gun goes to the less distance direction
        double gunTurn = (Math.min(Math.abs(gunInitialTurn), Rules.GUN_TURN_RATE)) * Math.signum(gunInitialTurn);

        // Set gun turn
        setTurnGunRight(gunTurn);

    }

    public void onScannedRobot(ScannedRobotEvent e) {

        setRadarTurn(e);
        setGunTurn(e);
        setFire(1);


        // PAINT debug

        scannedBot = true;
        // Calculate the angle and coordinates to the scanned robot
        double enemyAngle = getHeading() + e.getBearing();
        double enemyAngleRadians = Math.toRadians(enemyAngle);
        enemyLocation = getLocation(robotLocation, enemyAngleRadians, e.getDistance());

        // ----------
        if (enemy_heat > 0) {
            enemy_heat -= 0.1;
        }

        double energy_dec = enemy_energy - e.getEnergy();
        if (energy_dec > 0 && energy_dec <= 3) {
            double firepower = enemy_energy - e.getEnergy();
            bullets.add(new Bullet(enemyLocation, firepower, e.getDistance()));
            enemy_heat = 1 + (firepower / 5);
        }
        enemy_energy = e.getEnergy();
    }

    public Point2D getLocation(Point2D initLocation, double angle, double distance) {
        double x = (int) (initLocation.getX() + Math.sin(angle) * distance);
        double y = (int) (initLocation.getY() + Math.cos(angle) * distance);
        return new Point2D.Double(x, y);

    }

    public void onPaint(Graphics2D g) {
        // robot size = 40

        // Draw robot's security zone
        g.setColor(Color.green);
        drawCircle(g, getX(), getY(), 60);

        // Draw enemy robot and distance
        if (enemyLocation != null) {
            g.setColor(new Color(0xff, 0, 0, 0x80));
            drawLine(g, robotLocation, enemyLocation);
//			g.fillRect(x - 20, y - 20, 40, 40);
            drawBulletsRange(g);
        }
    }

    public void drawLine(Graphics2D g, Point2D source, Point2D target) {
        int sourceX = (int) source.getX();
        int sourceY = (int) source.getY();
        int targetX = (int) target.getX();
        int targetY = (int) target.getY();
        g.drawLine(sourceX, sourceY, targetX, targetY);
    }

    public void drawCircle(Graphics2D g, double x, double y, double radius) {
        int circumference = (int) (2 * radius);
        g.drawOval((int) (x - radius), (int) (y - radius), circumference, circumference);
    }

    public void drawBulletsRange(Graphics2D g) {
        for (Bullet bullet : bullets) {
            bullet.drawBulletRadius(g);
        }
    }
}

#pragma once

// MATH
#include "calculations.h"
#include "convert.h"
#include "quaternion.h"
#include "vector6d.h"

// PLANNER
#include "planner/pathPlanner.hpp"

// SOLVER
// Removed
#include "util/jointConfig.hpp"
#include "util/jointNode.hpp"

namespace planner {

namespace BlendCost {

class Planner : public PathPlanner
{
public:
    /**
     * @brief Constructor of the Blended Path Cost planner
     *
     * @param kinematic A shared pointer to a kinematic model
     * @param model A shared pointer to a physical model
     * @param solver A shared pointer to a kinematic solver
     */
    Planner(std::shared_ptr<kinematics::Kinematics> kinematic, std::shared_ptr<model::Model> model, std::shared_ptr<solver::InverseKinematics> solver);

    /**
     * Default destructor
     *
     */
    ~Planner();

    /**
     * @brief setMarginRatios: Set the list of multipliers for margin
     *
     * List of multipliers used for generating non-colliding waypoint alternatives in addWaypoint.
     * More multipliers give more alternatives to the solver for non-colliding points, but also increases computational cost
     * The final margin value depends on the margin value and the multiplier:
     * totalMargin = margin * marginRatio
     *
     * @param marginRatios: List if margin multipliers to be set
     */
    void setMarginRatios(const std::vector<double>& marginRatios);

    /**
     * @brief setMargin: Set the base margin for adding non-colliding points
     *
     * @param margin: The value of the obstacle avoidance margin in [mm]
     */
    void setMargin(double margin);

    /**
     * @brief setAllowTilting: Enable or disable tilt checking during path planning.
     * @param allowTilting: Boolean value to set tilt checking
     */
    void setAllowTilting(bool allowTilting);

    /**
     * @brief getWaypointBlends: Returns the list of blends for the current path
     *
     * This function returns this list of blends, where each element belongs to the waypoint in the same list position.
     *
     * @return List of blend values for the waypoints
     */
    std::vector<double>& getWaypointBlends();

    /**
     * @brief planMultiSegment: 
     * @param waypoints:
     * @param planMode:
     * @param segments:
     * @return
     */
    util::StatusCode planMultiSegment(const std::vector<std::vector<util::JointConfig>>& waypoints, std::vector<util::PlanMode>& planMode, std::vector<std::vector<util::JointConfig>>& segments);

private:

    // Removed

    /**
     * @brief quatSTDecomp: Swing-Twist decomposition of quaternions
     *
     * Used for tilt checking.
     *
     * @param rotation: The quaternion rotation to be decomposed
     * @param twistAxis: The desired axis for the twist
     * @param swing: Resultant quaternion for the swing
     * @param twist: Resultant quaternion for the twist
     */
    void quatSTDecomp(const opg::math::quaternion& rotation, const opg::math::vector3d& twistAxis, opg::math::quaternion& swing, opg::math::quaternion& twist);

public:
    static constexpr double COST_MAX = 1.0e15;

private:

    // Removed

    std::vector<double> marginRatios_ = { 1.0, 2.0, 3.0, 4.0 };
    std::vector<double> waypointBlends_;

    double pathMargin_               = 50.0;
    
    // Removed

    bool allowTilting_               = false;
    
    
    // Removed

};

} // namespace BlendCost

} // namespace planner

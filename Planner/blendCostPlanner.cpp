#include "planner/blendCost/blendCostPlanner.hpp"

#include <map>

namespace planner {

namespace BlendCost {

Planner::Planner(std::shared_ptr<kinematics::Kinematics> kinematic, std::shared_ptr<model::Model> model, std::shared_ptr<solver::InverseKinematics> solver)
    : PathPlanner(kinematic, model, solver)
{
    // Removed
}

Planner::~Planner() {}

void Planner::setMarginRatios(const std::vector<double>& marginRatios)
{
    marginRatios_ = marginRatios;
}

void Planner::setMargin(double margin)
{
    pathMargin_ = margin;
}

void Planner::setAllowTilting(bool allowTilting)
{
    allowTilting_ = allowTilting;
}

std::vector<double>& Planner::getWaypointBlends()
{
    return waypointBlends_;
}


// Removed


util::StatusCode Planner::planMultiSegment(const std::vector<std::vector<util::JointConfig>>& waypoints, std::vector<util::PlanMode>& planMode, std::vector<std::vector<util::JointConfig>>& segments)
{
    std::vector<std::vector<util::JointNode>> allNodes;

    // Check collisions
    for (size_t layerIdx = 0; layerIdx < waypoints.size(); layerIdx++)
    {
        const std::vector<util::JointConfig>& currentLayer = waypoints.at(layerIdx);
        std::vector<util::JointNode> layerNodes;

        for (size_t nodeIdx = 0; nodeIdx < currentLayer.size(); nodeIdx++)
        {
            const util::JointConfig& currentConfig = currentLayer.at(nodeIdx);
            if(!isCollision(currentConfig)) // TODO: ADD tilt check
            {
                layerNodes.push_back(util::JointNode(currentConfig, layerIdx, planMode.at(layerIdx)));
            }
        }

        // Not sure move is actually executed or this still does a copy
        allNodes.push_back(std::move(layerNodes));
    }

    /* TEST
     *
        #include <iostream>
        #include <memory>
        #include <chrono>
        #include <vector>

        using namespace std;

        class myClass
        {
            public:

            myClass() : justAPointer(NULL) {}

                int stuff = 2;
                float moreStuff = 10.0;
                shared_ptr<myClass> justAPointer;
        };

        int main()
        {
            std::vector<std::vector<myClass>> stuff;

            auto start = std::chrono::high_resolution_clock::now();

            for (size_t size1 = 0; size1 < 1000; size1++)
            {
                std::vector<myClass> oneStuff;
                for (size_t size2 = 0; size2 < 1000; size2++)
                {
                    oneStuff.push_back(myClass());
                }
                stuff.push_back(oneStuff);
            }

            auto end = std::chrono::high_resolution_clock::now();

            auto duration = std::chrono::duration_cast<std::chrono::microseconds>(end - start);

            cout << duration.count() << endl;

            return 0;
        }
        *
        *
        END OF TEST */


    // Link layers
    for (size_t layerIdx = 0; layerIdx < allNodes.size() - 1; layerIdx++)
    {
        std::vector<util::JointNode>& fromLayer = allNodes.at(layerIdx);
        std::vector<util::JointNode>& toLayer = allNodes.at(layerIdx + 1);

        for (util::JointNode& fromNode : fromLayer)
        {
            for (util::JointNode& toNode : toLayer)
            {
                fromNode.addNext(toNode);
            }
        }
    }

    // Graph optimization
    util::CostHeap costHeap;
    util::JointNode* currentNode;
    std::vector<util::JointConfig>* prevSegment;
    std::vector<util::JointConfig> tempCombinedSegments;
    std::vector<util::JointNode*>* neighborNodes;

    // Calculate first layer
    for (size_t startNodeIdx = 0; startNodeIdx < allNodes.size(); startNodeIdx++)
    {
        // Get neighbors of point
        currentNode = &(allNodes.front().at(startNodeIdx));
        neighborNodes = currentNode->getNext();

        for (size_t nodeIdx = 0; nodeIdx < neighborNodes->size(); nodeIdx++)
        {
            util::JointNode* currentNeighbor = neighborNodes->at(nodeIdx);
            // Calculate costs of all neighbors
            double neighborCost = currentNode->getCost() + linearCost(*currentNode, *currentNeighbor);

            // If the current cost of the neighbor is bigger than the new cost (or has no cost), then we assign the new cost and the current node
            if (currentNeighbor->getCost() > neighborCost || currentNeighbor->getCost() < 1e-15)
            {
                currentNeighbor->setCost(neighborCost);
                currentNeighbor->setPrev(currentNode);

                costHeap.push_heap(util::HeapElement(neighborCost, currentNeighbor));
            }
        }
    }

    currentNode = costHeap.pop_heap();

    util::JointNode* prevCurrent;

    // We repeat this until we find a node with no neighbor (which means we are on the cheapest end node)
    while (true)
    {
        std::cout << "Iterating through graph" << std::endl;
        std::cout << "Size of neighbors: " << neighborNodes->size() << std::endl;
        std::cout << "Current depth: " << currentNode->getLayer() << std::endl;
        std::cout << "Current node type: " << (int)currentNode->getPlanMode() << std::endl;
        std::cout << "Neighbor node type: " << (int)neighborNodes->front()->getPlanMode() << std::endl;

        if (neighborNodes->front()->getPlanMode() == util::PlanMode::STATIC)
        {
            // This can be multiple threads, calculating all neighbors is necessary
            for (size_t nodeIdx = 0; nodeIdx < neighborNodes->size(); nodeIdx++)
            {
                util::JointNode* currentNeighbor = neighborNodes->at(nodeIdx);
                // Calculate costs of all neighbors
                double neighborCost = currentNode->getCost() + linearCost(*currentNode, *currentNeighbor);

                // If the current cost of the neighbor is bigger than the new cost (or has no cost), then we assign the new cost and the current node
                if (currentNeighbor->getCost() > neighborCost || currentNeighbor->getCost() < 1e-15)
                {
                    currentNeighbor->setCost(neighborCost);
                    currentNeighbor->setPrev(currentNode);

                    costHeap.push_heap(util::HeapElement(neighborCost, currentNeighbor));
                }
            }

            currentNode = costHeap.pop_heap();
        }

        // Similar to the static case, but we do collision avoidance instead of just calculating the cost between nodes
        else if (neighborNodes->front()->getPlanMode() == util::PlanMode::DYNAMIC)
        {
            prevSegment = currentNode->getPrevSegment();

            util::StatusCode result = calculateDynamic(currentNode, neighborNodes);

            // This can be multiple threads, calculating all neighbors is necessary
            for (size_t nodeIdx = 0; nodeIdx < neighborNodes->size(); nodeIdx++)
            {
                util::JointNode* currentNeighbor = neighborNodes->at(nodeIdx);
                // Calculate costs of all neighbors                
                std::vector<util::JointConfig> currentSegment = std::vector<util::JointConfig>{*currentNode, *currentNeighbor};
                double neighborCost = currentNode->getCost() + linearCost(*currentNode, *currentNeighbor); // pre-calculate linear cost. If linear cost is higher than already est cost, then skip

                if (neighborCost > currentNeighbor->getCost() && currentNeighbor->getCost() > 1e-15)
                {
                    continue;
                }



                // Get previous segment points if any
                if (prevSegment->size() != 0)
                {
                    tempCombinedSegments.clear();
                    tempCombinedSegments.insert(tempCombinedSegments.end(), prevSegment->begin(), prevSegment->end() - 1); // skip last wp of the prev segment bc it's included in the current
                    tempCombinedSegments.insert(tempCombinedSegments.end(), currentSegment.begin(), currentSegment.end());
                    neighborCost = currentNode->getPrev()->getCost() + blendedSegmentSequenceCost(tempCombinedSegments); // This is not a good way to do it, the stopping cost gets acumulated
                }
                // We come from a static node, no extra waypoints
                else
                {
                    neighborCost = currentNode->getCost() + blendedSegmentSequenceCost(currentSegment);
                }

                // If the current cost of the neighbor is bigger than the new cost (or has no cost), then we assign the new cost and the current node
                if (currentNeighbor->getCost() > neighborCost || currentNeighbor->getCost() < 1e-15)
                {
                    currentNeighbor->setCost(neighborCost);
                    currentNeighbor->setPrev(currentNode);
                    currentNeighbor->setPrevSegment(std::move(currentSegment));

                    costHeap.push_heap(util::HeapElement(neighborCost, currentNeighbor));
                }
            }

            currentNode = costHeap.pop_heap();
        }

        neighborNodes = currentNode->getNext();

        if (neighborNodes->size() == 0)
        {
            break; // eh.
        }
    }

    // Traverse the graph backwards to find the best solution
    // Format of output:
    // [number of waypoints] -> [intra segment points + end point] (so technically, we just add the collision avoidance points to the waypoint where the segment ends)
    std::vector<util::JointConfig> currentSegment;
    util::JointNode* prevNode;

    while (true)
    {
        prevNode = currentNode->getPrev();
        if (prevNode == nullptr)
        {
            // We are done, exit
            break;
        }

        prevSegment = currentNode->getPrevSegment();

        std::cout << prevSegment->size() << std::endl;

        if (prevSegment != 0)
        {
            currentSegment.insert(currentSegment.end(), prevSegment->begin(), prevSegment->end());
        }

        currentSegment.push_back(*currentNode);

        segments.push_back(currentSegment);

        currentNode = prevNode;

        currentSegment.clear();
    }

    std::reverse(segments.begin(), segments.end());

    return util::StatusCode::NO_ERROR;
}


// Removed


void Planner::quatSTDecomp(const opg::math::quaternion& rotation, const opg::math::vector3d& twistAxis, opg::math::quaternion& swing, opg::math::quaternion& twist)
{
    if (twistAxis.norm() < UNIT_THRESH)
    {
        throw util::Exception("quadSTDecomp: Non-unit vector given as twist axis.");
    }

    opg::math::vector3d projectedAxis = twistAxis * opg::math::dot(twistAxis, rotation.imag());

    // if rotation and the twist axis are perpendicular (twist=0) we return the original rotation (swing = rotation)
    // This needs to be this high because opg::math has an 1e-2 threshold for norms
    if (projectedAxis.norm2() < 1e-4)
    {
        swing = rotation;
        twist = opg::math::quaternion(1.0, opg::math::vector3d(0.0, 0.0, 0.0));
        return;
    }

    twist                           = opg::math::quaternion(rotation.real(), projectedAxis);
    opg::math::quaternion twistConj = twist.normalize();
    swing                           = rotation * twistConj.conjugate();
}

// Removed

} // namespace BlendCost
} // namespace planner

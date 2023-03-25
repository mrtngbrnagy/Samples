#include "util/jointNode.hpp"

namespace util
{

JointNode::JointNode(JointConfig config, size_t nodeIdx, PlanMode planMode) : JointConfig(config), nodeIdx_(nodeIdx), planMode_(planMode){}

void JointNode::addNext(JointNode& toNode)
{
    next_.push_back(&toNode);
}

std::vector<JointNode*>* JointNode::getNext()
{
    return &next_;
}

PlanMode JointNode::getPlanMode()
{
    return planMode_;
}

double JointNode::getCost()
{
    return cost_;
}

void JointNode::setCost(double cost)
{
    cost_ = cost;
}

void JointNode::setPrev(util::JointNode* prevNode)
{
    prev_ = prevNode;
}

void JointNode::setPrevSegment(std::vector<util::JointConfig>&& prevSegment)
{
    prevSegment_ = prevSegment;
}

std::vector<util::JointConfig>* JointNode::getPrevSegment()
{
    return &prevSegment_;
}

util::JointNode* JointNode::getPrev()
{
    return prev_;
}

size_t JointNode::getLayer()
{
    return nodeIdx_;
}

}

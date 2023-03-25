 #pragma once

#include <vector>

#include "util/jointConfig.hpp"

namespace util {

enum class PlanMode{ STATIC, DYNAMIC };

class JointNode : public JointConfig
{
public:
    JointNode(JointConfig config, size_t nodeIdx, PlanMode planMode);

    void addNext(JointNode& toNode);

    std::vector<JointNode*>* getNext();

    PlanMode getPlanMode();

    double getCost();

    void setCost(double cost);

    void setPrev(util::JointNode* prevNode);
    util::JointNode* getPrev();
    void setPrevSegment(std::vector<util::JointConfig>&& prevSegment);
    std::vector<util::JointConfig>* getPrevSegment();

    size_t getLayer();

private:
    double poseClearance_;
    PlanMode planMode_;

    size_t nodeIdx_;

    std::vector<JointNode*> next_;
    util::JointNode* prev_ = nullptr;

    std::vector<util::JointConfig> prevSegment_;

    double cost_ = 0.0;
    double segmentCost_ = 1.0e15;


    // TODO: How to track heuristic cost?
};

class HeapElement
{
public:
    double cost_;
    JointNode* ptr_ = nullptr;
    size_t idx_ = 0;

    HeapElement(double cost, JointNode* ptr) : cost_(cost), ptr_(ptr){}

    HeapElement(double cost, size_t idx) : cost_(cost), idx_(idx) {}

    static bool comp(HeapElement& lhs, HeapElement& rhs)
    {
        return lhs.cost_ > rhs.cost_;
    }
};

class CostHeap : public std::vector<HeapElement>
{
public:
    void push_heap(util::HeapElement heapElem) // no ref for lvalue elisioning
    {
        push_back(heapElem);
        std::push_heap(begin(), end(), &util::HeapElement::comp);
    }

    util::JointNode* pop_heap()
    {
        util::JointNode* ptr = front().ptr_;
        std::pop_heap(begin(), end(), &util::HeapElement::comp);
        pop_back();

        return ptr;
    }

    bool pop_heap_idx(size_t& idx)
    {
        if (size() > 0)
        {
            idx = front().idx_;
            std::pop_heap(begin(), end(), &util::HeapElement::comp);
            pop_back();

            return true;
        }

        return false;
    }
};

} // namespace util

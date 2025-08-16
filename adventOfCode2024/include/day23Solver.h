#pragma once

#include "solver.h"
#include <cstdint>
#include <map>
#include <set>
#include <string>
#include <vector>

using NodeKey = int16_t;
using NodeLinks = std::map<NodeKey, std::set<NodeKey>>;
using NodeTriple = std::set<NodeKey>;
using NodeTripleSet = std::set<NodeTriple>;

class day23Solver : public solver {
private:
	std::vector<std::string> m_data;

    void getTripleSet(NodeTripleSet& dest, NodeKey start, const NodeLinks &forward,
                                   const NodeLinks &reverse, NodeKey second = -1, NodeKey third = -1);

public:
	day23Solver(const std::string& testFile);

    virtual solveResult compute();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
    virtual std::string computeString();
};
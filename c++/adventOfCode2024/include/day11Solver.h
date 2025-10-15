#pragma once

#include <map>
#include "solver.h"

class day11Solver : public solver {
private:
	std::vector<solveResult> m_data;
	std::map<std::pair<solveResult, int>, solveResult> m_resultCache;

	solveResult calcCount(solveResult seed, int iteration);

public:
	day11Solver(const std::string& testFile);

    virtual solveResult compute();
    virtual solveResult compute2();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};


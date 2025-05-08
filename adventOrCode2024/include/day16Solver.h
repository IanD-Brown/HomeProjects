#pragma once

#include "solver.h"
#include <set>

class day16Solver : public solver {
private:
	friend struct PathFinder;
	std::vector<std::string> m_data;
	coordinate m_start;
	coordinate m_end;


public:
	day16Solver(const std::string& testFile);

    virtual solveResult compute();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};


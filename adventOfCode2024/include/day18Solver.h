#pragma once

#include "solver.h"
#include "grid.h"

struct MemoryGrid;

class day18Solver : public solver {
private:
	std::vector<std::string> m_data;
	size_t m_limit;
	size_t m_count;
	MemoryGrid *m_grid;
	std::vector<std::string> m_overflow;

public:
	day18Solver(const std::string& testFile);

    virtual solveResult compute();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};
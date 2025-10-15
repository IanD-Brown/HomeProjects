#pragma once

#include "solver.h"

class day7Solver : public solver {
private:
	std::vector<std::string> m_data;

    virtual solveResult compute();
    virtual solveResult compute2();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);

	void computeLine(const std::string& line, solveResult* dest);

protected:

public:
    day7Solver(const std::string& testFile);
};


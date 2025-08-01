#pragma once
#include <map>
#include <set>

#include "solver.h"

class day14Solver : public solver {
private:
    struct Robot {
		const coordinate m_startLocation;
		const std::pair<solveResult, solveResult> m_velocity;

		Robot(const coordinate &m_startLocation, const coordinate &m_velocity) :
			m_startLocation(m_startLocation), m_velocity(m_velocity) {}
	};
	std::vector<Robot> m_data;

public:
	day14Solver(const std::string& testFile);

    virtual solveResult compute();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};


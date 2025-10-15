#pragma once

#include "solver.h"

class day17Solver : public solver {
private:
	struct Processor {
		solveResult m_a;
		solveResult m_b;
		solveResult m_c;
		std::vector<int> m_program;

		Processor();

		void reset();

		std::string run();

		solveResult value(int operand) const;
	};
	Processor m_processor;
	std::string m_program;

public:
	day17Solver(const std::string& testFile);

    virtual solveResult compute() { return 0; }

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);

	virtual std::string computeString();
};
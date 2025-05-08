#pragma once
#include <set>

#include "solver.h"

class day6Solver : public solver {
private:
  std::set<coordinate> m_blocks;
    size_t m_rowCount;
    size_t m_colCount;

    virtual solveResult compute();
    virtual solveResult compute2();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);

protected:

public:
    day6Solver(const std::string& testFile);
};

